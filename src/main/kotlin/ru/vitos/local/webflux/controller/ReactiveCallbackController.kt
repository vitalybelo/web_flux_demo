package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.CallbackTypes
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.INTERNAL_SERVER_ERROR
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.ReactiveCallbackStore

@RestController
@RequestMapping("/webflux/mono")
class ReactiveCallbackController(

    private val reactiveStore: ReactiveCallbackStore
) {


    /**
     * Принимает callback - с которым передается информация пользователя, (как-бы) запрошенная
     * на стороннем сервисе по идентификатору пользователя.
     *
     * @param userInfo информация пользователя
     * @return статус и полученную информацию
     */
    @PostMapping("/customer/callback")
    suspend fun callbackUserInfo(
        @RequestBody(required = false) userInfo: CustomerInfo?): Mono<ResponseEntity<Any>> {

        userInfo?.userId?.let { userId ->
            reactiveStore
                .insertCallbackData(userId, CallbackTypes.USER_INFO.name, userInfo)?.let {
                    val mono =
                        Mono.just(ResponseEntity<Any>(it, HttpStatus.OK))
                    mono.subscribe()
                    return mono
                }
            return internalServerError()
        }
        return invalidParameters()
    }




    suspend fun invalidParameters(): Mono<ResponseEntity<Any>> {

        val errorMono =
            Mono.just(ResponseEntity<Any>(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST))
        errorMono.subscribe { log.info(INVALID_PARAMETERS) }
        return errorMono
    }

    suspend fun internalServerError(): Mono<ResponseEntity<Any>> {

        val errorMono =
            Mono.just(ResponseEntity<Any>(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST))
        errorMono.subscribe { log.info(INTERNAL_SERVER_ERROR) }
        return errorMono
    }




}
