package ru.vitos.local.webflux.controller

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/index")
class MainPageController {

    @GetMapping("", produces = [MediaType.TEXT_HTML_VALUE])
    fun index(): Mono<String> {
        return Mono.just("""
            <html>
                <body>
                    <div style="text-align: center; margin-top: 50px;">
                        <h1>Hello, World!</h1>
                        <p>WebFlux is working via Netty</p>
                    </div>
                </body>
            </html>
        """.trimIndent())
    }
}