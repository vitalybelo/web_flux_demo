package ru.vitos.local.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import ru.vitos.local.webflux.authorization.KeycloakJwtConverter
import ru.vitos.local.webflux.config.filters.KeycloakLogoutHandler
import ru.vitos.local.webflux.logging.Log


@Configuration
@EnableWebFluxSecurity
class WebSecurityConfig(

    private val keycloakLogoutHandler: KeycloakLogoutHandler,
) {

    companion object: Log()

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {

        http.csrf { it.disable() }
            .cors { it.disable() }
            .authorizeExchange { exchange ->

                exchange.pathMatchers(
                    "/customer/**",
                    "/mono/customer/**",
                    "/actuator/health"
                ).permitAll()

                exchange.pathMatchers("/admin/**").hasRole("ADMIN")
                exchange.pathMatchers("/**").authenticated()
                    .anyExchange()
                    .denyAll()

            }
            .oauth2Login(Customizer.withDefaults())
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt -> jwt.jwtAuthenticationConverter(KeycloakJwtConverter()) }
            }
            .logout {
                it.logoutHandler(keycloakLogoutHandler)
                it.logoutUrl("/logout")
            }

        return http.build()
    }

}