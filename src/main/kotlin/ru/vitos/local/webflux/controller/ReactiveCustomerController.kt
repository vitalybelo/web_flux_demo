package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.Constants.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.service.ReactiveCustomerService

@RestController
@RequestMapping("/mono/customer")
class ReactiveCustomerController(

    private val reactiveService: ReactiveCustomerService
) {


    /**
     * Метод возвращает информацию о пользователе по заданному идентификатору.
     * Информация пользователя ожидается от другого сервиса как callback
     *
     * @param userId идентификатор пользователя
     * @return информацию о пользователе CustomerInfo
     */
    @GetMapping("/{user_id}")
    fun getUserInfo(@PathVariable("user_id") userId: String?): Mono<ResponseEntity<Any>> {

        userId?.let { userId ->
            val mono = reactiveService.fetchUserInfoReactive(userId)
            return mono.map { data ->
                ResponseEntity(data, HttpStatus.OK)
            }
        }
        return Mono.just(ResponseEntity(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST))
    }


}