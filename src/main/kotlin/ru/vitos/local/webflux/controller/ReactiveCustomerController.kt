package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.ReactiveCustomerService

@RestController
@RequestMapping("/mono/customer")
class ReactiveCustomerController(

    private val reactiveService: ReactiveCustomerService
) {

    companion object: Log()

    /**
     * Метод возвращает информацию о пользователе по заданному идентификатору.
     * Информация пользователя ожидается от другого сервиса как callback
     *
     * @param userId идентификатор пользователя
     * @return информацию о пользователе CustomerInfo
     */
    @GetMapping("/{user_id}")
    fun getAsyncUserInfo(

        @PathVariable("user_id", required = true) userId: String
    ): Mono<ResponseEntity<CustomerInfo>> {

        return reactiveService.fetchUserInfoReactive(userId)
            .map { data ->
                ResponseEntity.ok(data)
            }
            .switchIfEmpty(
                // Явно возвращаем 404, если сервис вернул пустоту
                Mono.just(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).build())
            )
            .onErrorResume { ex ->
                logger.errorM(
                    "Error fetching user info :: message = ${ex.message}, cause = ${ex.cause}",
                    ex
                )
                Mono.just(ResponseEntity( HttpStatus.INTERNAL_SERVER_ERROR))
            }
    }
}