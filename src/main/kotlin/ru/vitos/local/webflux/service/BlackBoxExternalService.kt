package ru.vitos.local.webflux.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.scheduler.Schedulers
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import java.lang.System.currentTimeMillis
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@Service
class BlackBoxExternalService(

    @Value("\${callback.timeout:60}") private val callbackTimeout: Long,
    private val reactiveCallbackStore: ReactiveCallbackStore
) {

    private val delayTimeout = Duration.ofSeconds(callbackTimeout).toMillis()


    /**
     * Мы приходим в этот метод для того, чтобы отправить запрос в сервис "коробка", получить
     * от "коробки" идентификатор callback запроса, а далее создаем неблокирующий процесс для
     * ожидания ответа от коробки, либо прерываем выполнение по истечению времени ожидания
     *
     * @param userId идентификатор пользователя для запроса USER_INFO
     * @return результат или падает
     */
    fun fetchUserInfoData(userId: String, callback: (Result<Any>) -> Unit) {

        val executor = Executors.newSingleThreadScheduledExecutor()
        val correlationId = requestUserInfoData(userId)

        // здесь мы начинаем ждать callback
        val beginAwaiting = currentTimeMillis()
        executor.scheduleAtFixedRate({
            try {
                // проверяем поступление callback и если поступил - читаем его
                reactiveCallbackStore.get(correlationId).publishOn(Schedulers.boundedElastic()).map { isReceived ->

                    if (isReceived) {
                        val monoRecord = reactiveCallbackStore.selectCallbackData(correlationId)
                        monoRecord.subscribe()
                        monoRecord.publishOn(Schedulers.boundedElastic()).map { data ->
                            if (data != null) {
                                executor.shutdown()
                                reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                                callback(Result.success(data))
                            }
                        }.subscribe()
                    }
                }.subscribe()

                // проверяем не истекло ли время ожидания
                val timeSpend = currentTimeMillis() - beginAwaiting
                if  (timeSpend > delayTimeout) {
                    // поймали тайм-аут - отваливаемся
                    executor.shutdown()
                    reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                    log.debug("Timeout was happen for correlationId = $correlationId >> $callbackTimeout seconds")
                    callback(Result.failure(TimeoutException()))
                }
                log.debug("Awaiting scheduler for correlationId = $correlationId is running ${timeSpend/1000} sec")

            } catch (e: Exception) {
                executor.shutdown()
                reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                callback(Result.failure(e))
            }
        }, 0L, 2L, TimeUnit.SECONDS)
    }


    /**
     * Сервис имитирует отправку запроса в сервис "коробка" на USER_INFO и сохраняет correlationId
     *
     * @param userId идентификатор пользователя для запроса USER_INFO
     * @return идентификатор запроса, полученный от сервиса "коробка"
     */
    fun requestUserInfoData(userId: String): String {

        // здесь мы как будто отправляем запрос в сервис "коробка" и как бы получаем идентификатор запроса
        // для упрощения - мы присвоим значение user_id идентификаторы обратного запроса, по которому далее
        // будет искать callback от сервиса "коробка" для USER_INFO
        val correlationId = userId
        log.info("Received BlackBox correlation id = $correlationId for awaiting callback")

        // сохраняем correlationId для ожидания обратного вызова от коробки
        reactiveCallbackStore.addAwaiting(correlationId).subscribe()
        return correlationId
    }

}