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

        http.authorizeExchange { exchangeSpec ->
            exchangeSpec.pathMatchers("/**").permitAll()
        }

        return http.build()
    }

}