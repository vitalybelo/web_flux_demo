package ru.vitos.local.webflux.authorization

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log


class KeycloakJwtConverter: Converter<Jwt, Mono<AbstractAuthenticationToken>> {


    companion object: Log()


    /**
     * Метод конвертирует realm роли, которые назначены пользователю в список authorities для цепочки
     * фильтров конфигурации безопасности приложения
     */
    override fun convert(jwt: Jwt): Mono<AbstractAuthenticationToken> {

        val authorities = try {

            // Безопасно извлекаем realm_access
            val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")

            // Безопасно кастим роли к списку строк
            @Suppress("UNCHECKED_CAST")
            val roles = realmAccess?.get("roles") as? List<String>

            // Если ролей нет — возвращаем пустой список, иначе маппим в GrantedAuthority
            roles?.map { role -> SimpleGrantedAuthority("ROLE_$role") }
                ?: emptyList()

        } catch (ex: Exception) {
            logger.errorM(">>>> Exception occurred while converting authorities of JWT, message = ${ex.message}, cause = ${ex.cause}")
            emptyList<GrantedAuthority>()
        }

        // Оборачиваем результат в Mono
        return Mono.just(JwtAuthenticationToken(jwt, authorities))
    }
}