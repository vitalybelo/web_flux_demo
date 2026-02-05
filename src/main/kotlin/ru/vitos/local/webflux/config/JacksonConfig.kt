package ru.vitos.local.webflux.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

/**
 * jacksonObjectMapper() — готовое расширение из jackson-module-kotlin, которое уже включает в себя KotlinModule
 * Vitalii Belotserkovskii (c), 05.02.2026
 */
@Configuration
class JacksonConfig {

    @Bean(name = ["objectMapper", "jacksonObjectMapper"])
    @Primary
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
    }
}