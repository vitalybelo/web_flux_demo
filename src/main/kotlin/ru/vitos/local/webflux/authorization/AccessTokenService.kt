package ru.vitos.local.webflux.authorization

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log
import java.util.*


/**
 * Класс для извлечения учетных данных и ролей пользователя из "Bearer" или "JSESSIONID" токенов доступа.
 * @author Belotserkovskii Vitalii, 24.03.2025
 */
@Service
@Suppress("unused")
class AccessTokenService(

    private val objectMapper: ObjectMapper
) {

    private var accessToken: AccessToken? = null

    companion object: Log() {
        const val TOKEN_PREFIX: String = "Bearer "
        const val AUTHORIZATION_HEADER: String = "Authorization"
    }


    /**
     * Проверяет доступность класса аутентификации spring security для чтения данных пользователя из токена доступа
     * @return true если security context доступен
     */
    private fun isSpringContext(): Mono<Boolean> {
        return ReactiveSecurityContextHolder.getContext()
            .map { context -> context.authentication != null }
            .defaultIfEmpty(false)
    }


    /**
     * Читает из контекста безопасности spring security класс аутентификации пользователя, если он там есть
     * @return инициализированный data класс AccessToken или null
     */
    suspend fun assign(): AccessToken? {
        return ReactiveSecurityContextHolder.getContext()
            .mapNotNull { context -> context.authentication }
            .mapNotNull { authentication -> assign(authentication) }
            .awaitSingleOrNull()
    }


    /**
     * В зависимости от источника запроса и типа токена (JWT или JSESSIONID), инициализирует авторизационный
     * класс AccessToken для чтения данных пользователя из токена аутентификации.
     *
     * @param authentication класс аутентификации spring boot security
     * @return инициализированный data класс AccessToken или null
     */
    fun assign(authentication: Authentication?): AccessToken? {

        authentication?.let {

            val principal = authentication.principal

            // Проверяем аутентификацию, выполненную по типу > BEARER
            if (principal is Jwt) {
                parseAccessToken(principal.tokenValue)?.let { return it }
            }

            // Проверяем аутентификацию, выполненную по типу > JSESSIONID
            if (principal is DefaultOidcUser) {
                parseAccessToken(principal.idToken.tokenValue)?.let { return it }
            }
        }
        return null
    }


    /**
     * Извлекает из заголовка http запроса токен доступа, и инициализирует с помощью него класс AccessToken
     * @param headers - карта заголовков http запроса
     * @return инициализированный data класс AccessToken или null
     */
    fun assign(headers: Map<String, String>): AccessToken? {

        getIgnoreCaseAuthorizationHeader(headers)?.let {
            return parseAccessToken(it)
        }
        return null
    }


    /**
     * Метод извлекает принципал из контекста безопасности spring security. В зависимости от источника
     * запроса и типа токена (JWT или JSESSIONID) извлекается карта с утверждениями токена
     * @return карта с утверждениями токена или пустая
    */
    suspend fun getClaims(): Map<String, Any> {

        val principal = ReactiveSecurityContextHolder.getContext()
            .mapNotNull { context -> context.authentication }
            .mapNotNull { authentication -> authentication.principal }
            .awaitSingleOrNull()

            if (principal is DefaultOidcUser) {
                return principal.claims
            }
            if (principal is Jwt) {
                return principal.claims
            }
        return emptyMap()
    }


    /**
     * Выполняет поиск среди ключей карты http заголовков - заголовка Authorization без учета регистра.
     * Если находит, извлекает значение заголовка, удаляет префикс и возвращает токен доступа
     *
     * @param headers - карта http заголовков
     * @return jwt токен доступа или null
     */
    private fun getIgnoreCaseAuthorizationHeader(headers: Map<String, String>): String? {
        headers.keys.stream()
            .filter { key -> key.equals(AUTHORIZATION_HEADER, true) }
            .findFirst().orElse(null)?.let { key ->
                headers[key]?.removePrefix(TOKEN_PREFIX)?.let {
                    return it
                }
            }
        return null
    }


    /**
     * Метод извлекает экземпляр класса авторизации AccessToken из payload токена доступа keycloak.

     * @param tokenString строка с токеном доступа keycloak, без префикса Bearer
     * @return экземпляр класса AccessToken, или null в случае ошибки
     */
    private fun parseAccessToken(tokenString: String?): AccessToken? {

        accessToken = null
        if (!tokenString.isNullOrEmpty()) {

            val decoder = Base64.getUrlDecoder()
            val chunks = tokenString.split(".")

            if (chunks.size > 1) {
                val payload = String(decoder.decode(chunks[1]))
                try {
                    objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    accessToken = objectMapper.readValue(payload, AccessToken::class.java)
                    accessToken?.tokenValue = tokenString

                } catch (ex: Exception) {
                    logger.errorM("Ошибка парсинга токена доступа :: message = ${ex.message}, cause = ${ex.cause}")
                }
            }
        }
        return accessToken
    }


    /**
     * @return извлекает и возвращает из карты ролей области все значения
     */
    suspend fun streamRealmRoles(): List<String> {
        return assign()?.realmRolesMap?.values?.flatten() ?: emptyList()
    }

    /**
     * @return извлекает и возвращает из карты ролей сервисов все значения
     */
    suspend fun streamClientRoles(): List<String> {
        return assign()?.clientRolesMap?.values?.flatMap { it.values.flatten() } ?: emptyList()
    }

}