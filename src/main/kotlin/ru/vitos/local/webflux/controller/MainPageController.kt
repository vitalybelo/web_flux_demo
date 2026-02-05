package ru.vitos.local.webflux.controller

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import ru.vitos.local.webflux.config.filters.KeycloakLogoutHandler
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.authorization.AccessTokenService
import ru.vitos.local.webflux.logging.Log
import java.net.URI

@Controller
class MainPageController(

    private val keycloakLogoutHandler: KeycloakLogoutHandler,
    private val accessTokenService: AccessTokenService
) {


    companion object: Log() {
        const val NO_DETECTED = "No detected"
    }

    @GetMapping("/index", produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun index(
        authentication: Authentication,
        model: Model
    ): String {

        val claims = accessTokenService.getClaims()
        val phone = accessTokenService.getClaims()["phone"] as String
        val position = accessTokenService.getClaims()["position"] as String

        logger.infoM("Phone = $phone")
        logger.infoM("Position = $position")
        logger.infoM("claims = $claims")

        val accessToken = accessTokenService.assign(authentication)
        val clientRoles = accessTokenService.streamClientRoles()
        val realmRoles = accessTokenService.streamRealmRoles()

        model.addAttribute("username", accessToken?.login ?: NO_DETECTED)
        model.addAttribute("first_name", accessToken?.firstName ?: NO_DETECTED)
        model.addAttribute("last_name", accessToken?.familyName ?: NO_DETECTED)
        model.addAttribute("phone", accessToken?.phone ?: NO_DETECTED)
        model.addAttribute("position", accessToken?.position ?: NO_DETECTED)
        model.addAttribute("department", accessToken?.department ?: NO_DETECTED)
        model.addAttribute("abscust_id", accessToken?.abscustId ?: NO_DETECTED)
        model.addAttribute("enter_time", accessToken?.enterTime ?: NO_DETECTED)
        model.addAttribute("method_2FA", accessToken?.required2FA ?: NO_DETECTED)
        model.addAttribute("full_name", accessToken?.fullName() ?: NO_DETECTED)
        model.addAttribute("client_roles", clientRoles)
        model.addAttribute("realm_roles", realmRoles)

        logger.info(model.toString())
        return "external"
    }


    @GetMapping("/custom-logout")
    suspend fun logout(

        exchange: ServerWebExchange,
        authentication: Authentication?
    ): ResponseEntity<Void> {

        if (authentication != null) {
            // Создаем WebFilterExchange вручную.
            val emptyChain = WebFilterChain { Mono.empty() }
            val filterExchange = WebFilterExchange(exchange, emptyChain)

            // keycloakLogoutHandler и ОБЯЗАТЕЛЬНО ждем (.awaitSingleOrNull)
            keycloakLogoutHandler.logout(filterExchange, authentication).awaitSingleOrNull()
        }

        // убиваем сессию локально и тоже ждем (.awaitSingleOrNull)
        exchange.session.flatMap { it.invalidate() }.awaitSingleOrNull()

        // Возвращаем обычный объект (без Mono обертки) - редирект на стартовую страницу
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create("/webflux/index"))
            .build()
    }
}