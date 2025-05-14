package ru.vitos.local.webflux.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.model.CustomerInfo
import ru.vitos.local.webflux.service.CallbackDataService

@RestController
@RequestMapping("/webflux")
class CallbackRestController(
    val callbackDataService: CallbackDataService,
) {


    @PostMapping("/customer/callback")
    fun confirmCustomerInfoRequest(@RequestBody(required = false) userInfo: CustomerInfo?): Mono<ResponseEntity<Any>> {

        userInfo?.userId?.let {
            callbackDataService.addCustomerInfoRequest(it, userInfo)
            return Mono.just(ResponseEntity(callbackDataService.getRequestMap(), HttpStatus.OK))
        }
        return Mono.just(ResponseEntity("Invalid parameters", HttpStatus.BAD_REQUEST))
    }



}