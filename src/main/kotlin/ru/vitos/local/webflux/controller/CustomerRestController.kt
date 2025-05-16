package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
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
    @RequestMapping(value = ["/customer/{user_id}","/customer"], method = [RequestMethod.GET] )
    suspend fun getCustomerInfo(
        @PathVariable("user_id", required = false) userId: String?): Mono<ResponseEntity<Any>> {

        userId?.let { userId ->
            return customerService.getCustomerInfo(userId)
        }
        val errorMono =
            Mono.just(ResponseEntity<Any>(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST))
        errorMono.subscribe { log.info(INVALID_PARAMETERS) }
        return errorMono
    }


}