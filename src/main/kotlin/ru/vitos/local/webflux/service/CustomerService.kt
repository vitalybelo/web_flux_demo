package ru.vitos.local.webflux.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.model.CustomerInfo
import java.lang.System.currentTimeMillis
import javax.management.timer.Timer

/**
 * Сервисный класс для получения информации пользователя через callback запрос от стороннего провайдера
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
class CustomerService(
    private val callbackDataService: CallbackDataService
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(CustomerService::class.java)
        const val TIMEOUT_DELAY = 60 * Timer.ONE_SECOND
    }

    /**
     * Метод возвращает информацию о пользователе, которую он получает из callback запроса.
     * На вход метода поступает идентификатор пользователя, далее метод ожидает поступления
     * callback от стороннего сервиса с информацией пользователя
     *
     * @param userId идентификатор пользователя
     * @return http ответ с информацией пользователя
     */
    fun getCustomerInfo(userId: String): Mono<ResponseEntity<Any>> {

        var userInfo: CustomerInfo?
        logger.info(">>>> getCustomerInfo by userId: $userId")

        val beginTimeoutMillis = currentTimeMillis()
        do {
            userInfo = callbackDataService.getCustomerByRequestId(userId)
            val deadlineTimeoutMillis = currentTimeMillis() - beginTimeoutMillis
            if  (deadlineTimeoutMillis > TIMEOUT_DELAY) {
                return Mono.just(ResponseEntity("Timeout", HttpStatus.REQUEST_TIMEOUT))
            }
        } while(userInfo == null)

        return Mono.just(ResponseEntity(userInfo, HttpStatus.OK))
    }

}