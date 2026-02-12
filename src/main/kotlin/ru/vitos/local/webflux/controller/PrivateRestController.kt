package ru.vitos.local.webflux.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vitos.local.webflux.authorization.AccessToken
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.service.KeycloakAdminService


@RestController
@RequestMapping("/private")
class PrivateRestController(

    private val keycloakAdminService: KeycloakAdminService
) {

    companion object : Log()

    /**
     * Выполняет парсинг access токена доступа и возвращает его клиенту
     * @return статус и токен доступа
     */
    @GetMapping("/token-info")
    suspend fun callbackTokenInfo(): ResponseEntity<AccessToken> {

        return keycloakAdminService.getTokenInfo()
    }

    /**
     * Выполняет парсинг access токена доступа, делает запрос на конечную точку userInfo
     * @return userInfo
     */
    @GetMapping("/user-info")
    suspend fun callbackUserInfo(): ResponseEntity<Map<String, Any>> {

        keycloakAdminService.getUserInfo()?.let { userInfoResponse ->

            logger.infoM("Request user info :: ${userInfoResponse.body}")
            return userInfoResponse
        }
        logger.errorM("Failed to get user info for user")
        return ResponseEntity.internalServerError().build()

    }

}
