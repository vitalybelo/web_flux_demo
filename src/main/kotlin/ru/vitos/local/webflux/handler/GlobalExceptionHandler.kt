package ru.vitos.local.webflux.handler

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import java.util.concurrent.TimeoutException


/**
 * Выполняет обработку кастомных исключений, для реализации бизнес логики
 * @author Vitalii Belotserkovskii, 18.04.2025
 */
@RestControllerAdvice
class GlobalExceptionHandler {



    @ExceptionHandler(TimeoutException::class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    fun handleTimeoutException() {
        log.error("Timeout occurred while getting delayed response")
    }

}