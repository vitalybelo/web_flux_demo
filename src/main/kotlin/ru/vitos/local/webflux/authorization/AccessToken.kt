package ru.vitos.local.webflux.authorization

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Класс описывающий сущность токена доступа пользователя keycloak. Поля по необходимости можно добавлять.
 * @author Vitalii Belotserkovskii, 24.03.2025
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessToken(

    var exp: Long? = null,
    var iat: Long? = null,
    var jti: String? = null,
    var iss: String? = null,
    var aud: List<String>? = null,

    @JsonProperty("sub") var userId: String? = null,
    @JsonProperty("typ") var type: String? = null,
    @JsonProperty("azp") var clientId: String? = null,
    @JsonProperty("sid") var sessionId: String? = null,
    @JsonProperty("acr") var acr: String? = null,
    @JsonProperty("allowed-origins") var allowedOrigins: List<String>? = null,

    @JsonProperty("realm_access")
    var realmRolesMap: LinkedHashMap<String, List<String>>? = null,

    @JsonProperty("resource_access")
    var clientRolesMap: LinkedHashMap<String, LinkedHashMap<String, List<String>>>? = null,

    @JsonProperty("scope") var scope: String? = null,
    @JsonProperty("given_name") var firstName: String? = null,
    @JsonProperty("middle_name") var middleName: String? = null,
    @JsonProperty("family_name") var familyName: String? = null,
    @JsonProperty("name") var displayName: String? = null,
    @JsonProperty("preferred_username") var login: String? = null,

    @JsonProperty("email") var email: String? = null,
    @JsonProperty("phone") var phone: String? = null,
    @JsonProperty("department") var department: String? = null,
    @JsonProperty("position") var position: String? = null,
    @JsonProperty("enter_time") var enterTime: String? = null,
    @JsonProperty("abscust_id") var abscustId: String? = null,
    @JsonProperty("required_2FA") var required2FA: String? = null,
    @JsonProperty("session_state") var sessionState: String? = null,

    @JsonProperty("email_verified") var emailVerified: Boolean = false,
    @JsonProperty("group_attribute") var groupAttribute: List<String>? = null,

    var tokenValue: String? = null

    ) {

    /**
     * @return Возвращает полное имя пользователя: имя, отчество и фамилию
     */
    fun fullName(): String {

        val space = " "
        val fio = StringBuilder().append(firstName ?: login ?: "Anonymous")

        if (!middleName.isNullOrBlank()) fio.append(space).append(middleName)
        if (!familyName.isNullOrBlank()) fio.append(space).append(familyName)

        return fio.toString()
    }

}