package ru.vitos.local.webflux.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.CustomerService

@RestController
@RequestMapping("/customer")
class CustomerRestController(

    private val customerService: CustomerService
) {

    companion object: Log()

    /**
     * Метод возвращает информацию о пользователе по заданному идентификатору.
     * Информация пользователя ожидается от другого сервиса как callback
     *
     * @param userId идентификатор пользователя
     * @return информацию о пользователе CustomerInfo
     */
    @GetMapping(value = ["/{user_id}"])
    suspend fun getCustomerInfo(

        @PathVariable("user_id", required = true) userId: String
    ): ResponseEntity<CustomerInfo> {

        if (userId.isNotEmpty()) {
            return customerService.awaitingCustomerInfo(userId)
        }
        logger.errorM("Parameter user_id is missing = \"$userId\"")
        return ResponseEntity.badRequest().build()
    }
}