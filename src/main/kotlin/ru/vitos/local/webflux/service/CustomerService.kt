package ru.vitos.local.webflux.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import javax.management.timer.Timer


/**
 * Сервисный класс для получения информации пользователя через callback запрос от стороннего провайдера
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
class CustomerService(

    @param:Value($$"${callback.total.timeout.seconds:60}")
    private val callbackTimeout: Long,

    private val callbackDataService: CallbackDataService
) {

    private val delayMillis: Long = 300L
    private val delayTimeout: Long = callbackTimeout * Timer.ONE_SECOND

    companion object: Log()

    /**
     * Метод возвращает информацию о пользователе, которую он получает из callback запроса.
     * На вход метода поступает идентификатор пользователя, далее метод ожидает поступления
     * callback от стороннего сервиса с информацией пользователя
     *
     * @param userId идентификатор пользователя
     * @return http ответ с информацией пользователя
     */
    suspend fun awaitingCustomerInfo(userId: String): ResponseEntity<CustomerInfo> {

        // здесь мы должны отправить запрос в коробку для получения callback и получить id ожидания
        val correlationId = userId
        logger.infoM("Starting awaiting callback for user id = $userId :: with timeout $delayTimeout")

        var userInfo: CustomerInfo? = null
        withTimeoutOrNull(delayTimeout) {

            while (coroutineContext.isActive) {
                userInfo = callbackDataService.fetchUserInfoCallback(correlationId)
                if (userInfo != null) return@withTimeoutOrNull
            }
            delay(delayMillis)
        }

        return if (userInfo != null) {
            logger.infoM("Callback received successfully for correlationId = $correlationId")
            ResponseEntity.ok(userInfo)
        } else {
            logger.warnM("Request timeout waiting for correlationId = $correlationId")
            ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build()
        }
    }
}