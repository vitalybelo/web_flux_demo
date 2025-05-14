package ru.vitos.local.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebFluxDemoApplication

fun main(args: Array<String>) {
    runApplication<WebFluxDemoApplication>(*args)
}
