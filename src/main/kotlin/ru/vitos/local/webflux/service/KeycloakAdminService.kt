package ru.vitos.local.webflux.service

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.vitos.local.webflux.authorization.AccessToken
import ru.vitos.local.webflux.authorization.AccessTokenService
import ru.vitos.local.webflux.logging.Log

@Service
class KeycloakAdminService(

    private val webClient: WebClient,
    private val accessTokenService: AccessTokenService
) {

    companion object: Log()


    /**
     * Выполняет чтение user info для пользователя (используется access токен, взятый из запроса)
     * Метод извлекает токен из reactive контекста безопасности. Формируется uri запроса в keycloak.
     * В качестве токена доступа пере-используется токен из заголовка начального запроса.
     *
     * @return карту user info
     */
    suspend fun getUserInfo(): ResponseEntity<Map<String, Any>>? {

        try {
            val accessToken = accessTokenService.assign() ?: return ResponseEntity.notFound().build()
            val issuer = accessToken.iss ?: return ResponseEntity.badRequest().build()

            val userInfo = webClient.get()
                .uri("$issuer/protocol/openid-connect/userinfo")
                .header("Authorization", "Bearer ${accessToken.tokenValue}")
                .retrieve()
                .awaitBody<Map<String, Any>>()

            return ResponseEntity.ok(userInfo)

        } catch (ex: Exception) {

            logger.errorM("Failed to get user info :: message = ${ex.message}", ex)
            return ResponseEntity.internalServerError().build()
        }
    }


    /**
     * Выполняет чтение токена доступа из контекста, и возвращает его клиенту
     * @return экземпляр класса AccessToken
     */
    suspend fun getTokenInfo(): ResponseEntity<AccessToken> {

        val accessToken = accessTokenService.assign() ?: return ResponseEntity.notFound().build()
        logger.infoM("Request access token info :: $accessToken")
        return ResponseEntity.ok(accessToken)
    }

}