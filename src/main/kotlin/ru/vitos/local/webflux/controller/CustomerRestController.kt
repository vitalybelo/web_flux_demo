package ru.vitos.local.webflux.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.service.CustomerService

@RestController
@RequestMapping("/webflux")
class CustomerRestController(
    private val customerService: CustomerService
) {

    /**
     * Метод возвращает информацию о пользователе по заданному идентификатору.
     * Информация пользователя ожидается от другого сервиса как callback
     *
     * @param userId идентификатор пользователя
     * @return информацию о пользователе CustomerInfo
     */
    @GetMapping("/customer/{user_id}")
    fun getCustomerInfo(@PathVariable("user_id") userId: String?): Mono<ResponseEntity<Any>> {

        return Mono.justOrEmpty(userId)
            .switchIfEmpty(Mono.error { IllegalArgumentException("User id is not specified") })
            .flatMap {
                userId -> customerService.getCustomerInfo(userId)
            }
    }


}