package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.CallbackTypes
import ru.vitos.local.webflux.constants.Constants.Companion.CORRELATION_ABSENT
import ru.vitos.local.webflux.constants.Constants.Companion.INTERNAL_SERVER_ERROR
import ru.vitos.local.webflux.constants.Constants.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.ReactiveCallbackStore

@RestController
@RequestMapping("/mono/customer")
class ReactiveCallbackController(

    private val reactiveStore: ReactiveCallbackStore
) {

    companion object: Log()

    /**
     * Принимает callback - с которым передается информация пользователя, (как-бы) запрошенная
     * на стороннем сервисе по идентификатору пользователя.
     *
     * @param userInfo информация пользователя
     * @return статус и полученную информацию
     */
    @PostMapping("/callback")
    suspend fun callbackUserInfo(
        @RequestBody(required = false) userInfo: CustomerInfo?): Mono<ResponseEntity<Any>> {

        userInfo?.userId?.let { userId ->

            try {
                reactiveStore
                    .insertCallbackData(userId, CallbackTypes.USER_INFO.name, userInfo)?.let {
                        val mono =
                            Mono.just(ResponseEntity<Any>(it, HttpStatus.OK))
                        mono.subscribe()
                        return mono
                    }
                return errorResponseMono(CORRELATION_ABSENT, HttpStatus.NOT_FOUND)
            } catch (ex: Exception) {
                logger.errorM("Error during inserting callback, message = ${ex.message}, cause = ${ex.cause}")
            }
            return errorResponseMono(INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return errorResponseMono(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST)
    }


    suspend fun errorResponseMono(
        errorText: String,
        errorHttpStatus: HttpStatus

    ): Mono<ResponseEntity<Any>> {

        val errorMono =
            Mono.just(ResponseEntity<Any>(errorText, errorHttpStatus))
        errorMono.subscribe { logger.errorM(errorText) }
        return errorMono
    }

}
