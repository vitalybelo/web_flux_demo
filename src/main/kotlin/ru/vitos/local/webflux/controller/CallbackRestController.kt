package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vitos.local.webflux.constants.Constants.Companion.INVALID_PARAMETERS
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.CallbackDataService

@RestController
@RequestMapping("/customer")
class CallbackRestController(

    private val callbackDataService: CallbackDataService
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

    ): ResponseEntity<CallbackTable> {

        userInfo.userId?.let { userId ->

            callbackDataService.addUserInfoCallback(userId, userInfo)?.let { record ->

                logger.infoM("Added info for user id = ${record.correlationId}")
                return ResponseEntity(record, HttpStatus.OK)
            }
        }
        logger.errorM("Corrupted user info data accepted = $userInfo")
        return ResponseEntity.badRequest().build()
    }














}
