package ru.vitos.local.webflux.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/webflux")
class MainPageController {

    @GetMapping("/")
    fun index(): Mono<String> {

        return Mono.just("<div><br><h1>Hello, World !</h1></div>")
    }

}