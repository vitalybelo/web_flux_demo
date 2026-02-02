package ru.vitos.local.webflux.config

import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

/**
 * Конфигурация R2DBC для выполнения реактивных запросов в БД
 * @author Vitalii Belotserkovskii, 16.05.2025
 */
@Configuration
@EnableR2dbcRepositories
class R2dbcConfig(

    @param:Value("\${r2dbc.host}")
    private val host: String,

    @param:Value("\${r2dbc.port}")
    private val port: Int,

    @param:Value("\${r2dbc.dbase}")
    private val dbase: String,

    @param:Value("\${r2dbc.username}")
    private val username: String,

    @param:Value("\${r2dbc.password}")
    private val password: String

) {

    @Bean
    fun connectionFactory(): ConnectionFactory {
        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(dbase)
                .username(username)
                .password(password)
                .build()
        )
    }

    @Bean
    fun r2dbcEntityTemplate(connectionFactory: ConnectionFactory): R2dbcEntityTemplate {
        return R2dbcEntityTemplate(connectionFactory)
    }

}