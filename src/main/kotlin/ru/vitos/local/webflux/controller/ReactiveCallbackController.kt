package ru.vitos.local.webflux.controller

import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.constants.CallbackTypes
import ru.vitos.local.webflux.constants.Constants.Companion.CORRELATION_ABSENT
import ru.vitos.local.webflux.entity.CallbackTable
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

        @RequestBody(required = true) userInfo: CustomerInfo
    ): Mono<ResponseEntity<CallbackTable>> {

        val userId = userInfo.userId ?: return Mono.just(ResponseEntity.badRequest().build())
        return mono {
            try {
                val savedData = reactiveStore.insertCallbackData(
                    userId,
                    CallbackTypes.USER_INFO.name,
                    userInfo
                )
                if (savedData != null) {
                    ResponseEntity.ok(savedData)
                } else {
                    logger.errorM(CORRELATION_ABSENT)
                    ResponseEntity(HttpStatus.NOT_FOUND)
                }
            } catch (ex: Exception) {
                logger.errorM("Error during inserting callback: ${ex.message}", ex)
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }
    }

}
