package ru.vitos.local.webflux.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.vitos.local.webflux.authorization.AccessToken
import ru.vitos.local.webflux.authorization.AccessTokenService
import ru.vitos.local.webflux.logging.Log


@RestController
@RequestMapping("/admin")
class AdminRestController(

    private val accessTokenService: AccessTokenService
) {

    companion object: Log()

    /**
     * Выполняет парсинг access токена доступа и возвращает его клиенту
     * @return статус и токен доступа
     */
    @GetMapping("/token-info")
    suspend fun callbackUserInfo(): ResponseEntity<AccessToken> {

        val accessToken = accessTokenService.assign()
            ?: return ResponseEntity.notFound().build()

        logger.infoM("Request access token info :: $accessToken")
        return ResponseEntity.ok(accessToken)

    }














}
