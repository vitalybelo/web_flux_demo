package ru.vitos.local.webflux.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.scheduler.Schedulers
import ru.vitos.local.webflux.logging.Log
import java.lang.System.currentTimeMillis
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.management.timer.Timer


@Service
class BlackBoxExternalService(

    @param:Value("\${callback.total.timeout.seconds:30}")
    private val callbackTimeout: Long,

    @param:Value("\${callback.memory.delay.seconds:2}")
    private val memoryDelay: Long,

    private val reactiveCallbackStore: ReactiveCallbackStore
) {

    companion object: Log()


    /**
     * Мы приходим в этот метод для того, чтобы отправить запрос в сервис "коробка", получить
     * от "коробки" идентификатор callback запроса, а далее создаем неблокирующий процесс для
     * ожидания ответа от коробки, либо прерываем выполнение по истечению времени ожидания
     *
     * @param userId идентификатор пользователя для запроса USER_INFO
     * @return результат или падает
     */
    fun fetchUserInfoData(userId: String, callback: (Result<Any>) -> Unit) {

        logger.infoM("Start fetching user info data for :: $userId")

        val executor = Executors.newSingleThreadScheduledExecutor()
        val correlationId = requestUserInfoData(userId)

        // здесь мы начинаем ждать callback
        val begin = currentTimeMillis()
        val timeout = currentTimeMillis().plus(callbackTimeout * Timer.ONE_SECOND)
        executor.scheduleAtFixedRate({
            try {
                // проверяем не наступил ли "тайм-аут"
                if (timeout < currentTimeMillis()) {
                    // поймали тайм-аут - отваливаемся
                    executor.shutdown()
                    reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                    logger.debugM("Timeout was happen for correlationId = $correlationId >> $callbackTimeout seconds")
                    callback(Result.failure(TimeoutException()))
                }

                // проверяем поступление callback и если поступил - читаем его
                reactiveCallbackStore.get(correlationId).publishOn(Schedulers.boundedElastic()).map { isReceived ->

                    if (isReceived) {
                        val monoRecord = reactiveCallbackStore.selectCallbackData(correlationId)
                        monoRecord.subscribe()
                        monoRecord.publishOn(Schedulers.boundedElastic()).map { data ->
                            if (data != null) {
                                executor.shutdown()
                                logger.infoM("Callback received for user info data :: $userId")
                                reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                                callback(Result.success(data))
                            }
                        }.subscribe()
                    }
                }.subscribe()

            } catch (e: Exception) {
                executor.shutdown()
                reactiveCallbackStore.removeAwaiting(correlationId).subscribe()
                callback(Result.failure(e))
            }
            if (logger.isDebugEnabled) {
                val spendTime =  (currentTimeMillis() - begin) / Timer.ONE_SECOND
                logger.debugM("Awaiting scheduler for correlationId = $correlationId is running $spendTime sec")
            }
        }, 0L, memoryDelay, TimeUnit.SECONDS)
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
        logger.infoM("Received BlackBox correlation id = $correlationId for awaiting callback")

        // сохраняем correlationId для ожидания обратного вызова от коробки
        reactiveCallbackStore.addAwaiting(correlationId).subscribe()
        return correlationId
    }

}