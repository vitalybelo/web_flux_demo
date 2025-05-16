package ru.vitos.local.webflux.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


/**
 * Примитивный дата класс информации пользователя
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerInfo(

    @JsonProperty("user_id") var userId: String? = null,
    @JsonProperty("user_name") var userName: String? = null,
    @JsonProperty("first_name") var firstName: String? = null,
    @JsonProperty("family_name") var familyName: String? = null,
    @JsonProperty("middle_name") var middleName: String? = null,

    var email: String? = null,
    var phone: String? = null

)