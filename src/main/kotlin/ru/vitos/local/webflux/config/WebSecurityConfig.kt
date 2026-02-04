package ru.vitos.local.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class WebSecurityConfig {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain? {

        http.csrf { it.disable() }
            .cors { it.disable() }
            .authorizeExchange { exchangeSpec ->

                exchangeSpec.pathMatchers("/admin/**").hasRole("ADMIN")

                exchangeSpec.pathMatchers("/private/**").authenticated()

                exchangeSpec.pathMatchers(
                    "/index",
                    "/customer/**",
                    "/mono/customer/**",
                    "/actuator/health"
                ).permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { } // Включает стандартную валидацию JWT
            }

        return http.build()
    }
}