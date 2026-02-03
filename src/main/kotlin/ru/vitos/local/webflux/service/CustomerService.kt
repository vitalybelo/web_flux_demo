package ru.vitos.local.webflux.service

import kotlinx.coroutines.delay
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import java.lang.System.currentTimeMillis
import javax.management.timer.Timer


/**
 * Сервисный класс для получения информации пользователя через callback запрос от стороннего провайдера
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
class CustomerService(

    @param:Value("\${callback.total.timeout.seconds:60}")
    private val callbackTimeout: Long,

    private val callbackDataService: CallbackDataService
) {

    private val delayTimeout = callbackTimeout * Timer.ONE_SECOND

    companion object: Log()

    /**
     * Метод возвращает информацию о пользователе, которую он получает из callback запроса.
     * На вход метода поступает идентификатор пользователя, далее метод ожидает поступления
     * callback от стороннего сервиса с информацией пользователя
     *
     * @param userId идентификатор пользователя
     * @return http ответ с информацией пользователя
     */
    suspend fun getCustomerInfo(userId: String): Mono<ResponseEntity<Any>> {

        var userInfo: CustomerInfo?
        val beginTimeoutMillis = currentTimeMillis()
        // здесь мы как-бы отправили запрос в коробку для получения callback и получили id
        val correlationId = userId

        logger.infoM("Starting getCustomerInfo for $userId :: with timeout $delayTimeout")
        // начинаем ждать callback
        do {
            userInfo = callbackDataService.getUserInfoCallback(correlationId)
            if (userInfo != null) break
            // пока callback не поступил, проверяем тайм-аут
            val deadlineTimeoutMillis = currentTimeMillis() - beginTimeoutMillis
            if  (deadlineTimeoutMillis > delayTimeout) {
                // поймали тайм-аут - отваливаемся
                val timeoutMono =
                    Mono.just(ResponseEntity<Any>("Timeout", HttpStatus.REQUEST_TIMEOUT))
                timeoutMono.subscribe {
                    logger.infoM("Timeout happen of callback correlationId = $correlationId")
                }
                return timeoutMono
            }
            delay(1000)

        } while(true)

        val successMono =
            Mono.just(ResponseEntity<Any>(userInfo, HttpStatus.OK))
        successMono.subscribe {
            logger.infoM("Success of callback waiting for user id = $userId")
        }
        return successMono
    }



}