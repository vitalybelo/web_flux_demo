package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.CallbackDataService

@RestController
@RequestMapping("/webflux")
class CallbackRestController(

    private val callbackDataService: CallbackDataService
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
            callbackDataService.addUserInfoCallback(userId, userInfo)?.let { record ->

                val successMono=
                    Mono.just(ResponseEntity<Any>(record, HttpStatus.OK))
                successMono.subscribe { log.info("Added info for user id = ${record.userId}") }
                return successMono
            }
        }
        val errorMono =
            Mono.just(ResponseEntity<Any>(INVALID_PARAMETERS, HttpStatus.BAD_REQUEST))
        errorMono.subscribe { log.info(INVALID_PARAMETERS) }
        return errorMono
    }














}
