package ru.vitos.local.webflux.config

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class LiquibaseConfig(

    @Value($$"${r2dbc.host}")val host: String,
    @Value($$"${r2dbc.port}") val port: String,
    @Value($$"${r2dbc.dbase}") val dbase: String,
    @Value($$"${r2dbc.username}") val user: String,
    @Value($$"${r2dbc.password}") val pass: String,
    @Value($$"${spring.liquibase.change-log}") val changeLogPath: String

) {

    @Bean
    fun liquibase(): SpringLiquibase {

        val jdbcUrl = "jdbc:postgresql://$host:$port/$dbase"

        val dataSource: DataSource = DataSourceBuilder.create()
            .driverClassName("org.postgresql.Driver")
            .url(jdbcUrl)
            .username(user)
            .password(pass)
            .build()

        val liquibase = SpringLiquibase()
        liquibase.dataSource = dataSource
        liquibase.changeLog = changeLogPath

        liquibase.defaultSchema = "public"
        liquibase.isDropFirst = false
        liquibase.setShouldRun(true)

        return liquibase
    }
}