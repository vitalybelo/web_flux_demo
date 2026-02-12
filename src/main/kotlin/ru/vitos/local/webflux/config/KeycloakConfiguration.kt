package ru.vitos.local.webflux.config

import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class KeycloakConfiguration (

    @Value($$"${keycloak.server.url:http://localhost:8443}")
    private val keycloakServerUrl: String,
    @Value($$"${keycloak.admin.realm:SpringBootKeycloak}")
    private val keycloakRealm: String,
    @Value($$"${keycloak.admin.client_id:login-admin}")
    private val keycloakAdminClientId: String,
    @Value($$"${keycloak.admin.client_secret}")
    private val keycloakAdminClientSecret: String,
    @Value($$"${keycloak.master.admin.realm}")
    private val keycloakMasterAdminRealm: String,
    @Value($$"${keycloak.master.admin.client-id}")
    private val keycloakMasterAdminClientId: String,
    @Value($$"${keycloak.master.admin.secret}")
    private val keycloakMasterAdminClientSecret: String,
) {

    @Bean
    fun keycloakBuilder(): KeycloakBuilder {
        return KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm(keycloakRealm)
            .clientId(keycloakAdminClientId)
            .clientSecret(keycloakAdminClientSecret)
    }

    @Bean
    @Qualifier("keycloak")
    fun keycloak(): Keycloak {
        return keycloakBuilder()
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build()
    }

    @Bean
    @Qualifier("keycloakMaster")
    fun keycloakMaster(): Keycloak {
        return KeycloakBuilder.builder()
            .serverUrl(keycloakServerUrl)
            .realm(keycloakMasterAdminRealm)
            .clientId(keycloakMasterAdminClientId)
            .clientSecret(keycloakMasterAdminClientSecret)
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .build()
    }

    @Bean
    fun realmResource(): RealmResource {
        return keycloakBuilder()
            .build()
            .realm(keycloakRealm)
    }

}