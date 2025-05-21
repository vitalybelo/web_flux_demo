package ru.vitos.local.webflux.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import ru.vitos.local.webflux.model.CustomerInfo
import java.lang.System.currentTimeMillis
import javax.management.timer.Timer


/**
 * Сервисный класс для получения информации пользователя через callback запрос от стороннего провайдера
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
class CustomerService(

    @Value("\${callback.timeout:60}") private val callbackTimeout: Long,
    private val callbackDataService: CallbackDataService
) {

    private val delayTimeout = callbackTimeout * Timer.ONE_SECOND


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
        log.info("Starting getCustomerInfo for $userId :: with timeout $delayTimeout")
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
                timeoutMono.subscribe { log.info("Timeout happen of callback correlationId = $correlationId") }
                return timeoutMono
            }
            Thread.sleep(1000)

        } while(true)

        val successMono =
            Mono.just(ResponseEntity<Any>(userInfo, HttpStatus.OK))
        successMono.subscribe { log.info("Success of callback waiting for $userId") }
        return successMono
    }



}