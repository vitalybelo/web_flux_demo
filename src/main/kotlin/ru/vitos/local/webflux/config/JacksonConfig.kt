package ru.vitos.local.webflux.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
open class JacksonConfig {

    @Bean(name = ["objectMapper", "jacksonObjectMapper"])
    @Primary
    open fun objectMapper(): ObjectMapper {
        // jacksonObjectMapper() — это готовое расширение из jackson-module-kotlin,
        // которое уже включает в себя KotlinModule
        return jacksonObjectMapper()
    }
}