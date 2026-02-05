package ru.vitos.local.webflux.config.filters

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log

@Component
class KeycloakLogoutHandler(

    private val webClient: WebClient
): ServerLogoutHandler {

    companion object: Log()

    override fun logout(
        exchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {

        return logoutFromKeycloak(authentication)
    }

    /**
     * Метод реализует выход из keycloak запросом по back channel
     * @param authentication - класс аутентификации Spring Boot Security
     */
    private fun logoutFromKeycloak(authentication: Authentication): Mono<Void> {

        val user = authentication.principal as? OidcUser ?: return Mono.empty()
        val issuer = user.issuer ?: return Mono.empty()
        val clientId = user.getClaimAsString("azp")

        val uri = UriComponentsBuilder
            .fromUriString("$issuer/protocol/openid-connect/logout")
            .queryParam("client_id", clientId)
            .queryParam("logout_hint", user.name)
            .queryParam("id_token_hint", user.idToken.tokenValue)
            .build()
            .toUri()

        // Отправляем GET запрос НЕБЛОКИРУЮЩИМ способом
        return webClient.get()
            .uri(uri)
            .retrieve()
            .toBodilessEntity() // Нам не нужно тело ответа, только статус
            .doOnSuccess {
                logger.infoM("Successfully logged out from Keycloak for user: ${user.name}")
            }
            .doOnError { error ->
                logger.errorM("Could not propagate logout to Keycloak: ${error.message}")
            }
            .then()
    }

}