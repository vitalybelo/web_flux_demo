package ru.vitos.local.webflux.service

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.logging.Log
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeoutException
import javax.management.timer.Timer


@Service
class BlackBoxExternalService(

    @param:Value($$"${callback.total.timeout.seconds:30}")
    private val callbackTimeout: Long,

    @param:Value($$"${callback.memory.delay.millis:300}")
    private val awaitingDelay: Long,

    private val reactiveCallbackStore: ReactiveCallbackStore
) {

    companion object: Log()
    private val timeoutInSeconds = callbackTimeout * Timer.ONE_SECOND

    /**
     * Мы приходим в этот метод для того, чтобы отправить запрос в сервис "коробка", получить
     * от "коробки" идентификатор callback запроса, а далее создаем неблокирующий процесс для
     * ожидания ответа от коробки, либо прерываем выполнение по истечению времени ожидания
     *
     * @param userId идентификатор пользователя для запроса USER_INFO
     * @return результат или падает
     */
    suspend fun fetchUserInfoData(userId: String): CallbackTable? {

        logger.infoM("Start fetching user info callback data for :: $userId")
        val correlationId = requestUserInfoFromBlackBox(userId)

        return try {
            // withTimeout автоматически выбросит TimeoutCancellationException, если время выйдет
            withTimeout(timeoutInSeconds) {
                // бесконечный цикл, пока не получим данные или не сработает "тайм-аут"
                val startCheck = currentTimeMillis()
                while (coroutineContext.isActive) {

                    reactiveCallbackStore.selectCallbackData(correlationId)?.let { record ->
                        logger.debugM("Callback received for userId = $userId :: record = $record")
                        return@withTimeout record
                    }
                    if (logger.isDebugEnabled) {
                        val seconds = (currentTimeMillis() - startCheck).toFloat() / 1000
                        logger.debugM("Awaiting callback for correlationId = $correlationId :: $seconds seconds")
                    }
                    // ждем перед следующей проверкой
                    delay(awaitingDelay)
                }
                return@withTimeout null // сюда никогда не попадем, но обмануть компилятор придется
            }

        } catch (ex: TimeoutCancellationException) {
            // Преобразуем системное исключение корутин в понятное бизнес-исключение
            logger.errorM("Timeout for correlationId = $correlationId :: message = $ex.message, cause = ${ex.cause}")
            throw TimeoutException("Timeout fetching data for user $userId")

        } catch (ex: Exception) {
            logger.errorM("Error while fetching user info", ex)
            throw ex

        } finally {
            reactiveCallbackStore.removeAwaiting(correlationId)
        }
    }


    /**
     * Сервис имитирует отправку запроса в сервис "коробка" на USER_INFO и сохраняет correlationId
     *
     * @param userId идентификатор пользователя для запроса USER_INFO
     * @return идентификатор запроса, полученный от сервиса "коробка"
     */
    suspend fun requestUserInfoFromBlackBox(userId: String): String {

        logger.infoM("Requesting correlation id for userId= $userId from Black Box")
        // здесь мы как будто отправляем запрос в сервис "коробка" и как бы получаем идентификатор запроса
        // для упрощения - мы присвоим значение user_id идентификатору обратного запроса, по которому далее
        // будем искать callback от сервиса "коробка" для USER_INFO
        val correlationId = userId
        logger.infoM("Received Black Box correlation id = $correlationId for awaiting callback")

        // сохраняем correlationId для ожидания обратного вызова от коробки
        reactiveCallbackStore.addAwaiting(correlationId)
        return correlationId
    }

}