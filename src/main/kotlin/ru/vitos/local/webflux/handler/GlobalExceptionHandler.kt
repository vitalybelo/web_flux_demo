package ru.vitos.local.webflux.handler

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.vitos.local.webflux.logging.Log
import java.util.concurrent.TimeoutException


/**
 * Выполняет обработку кастомных исключений, для реализации бизнес логики
 * @author Vitalii Belotserkovskii (c), 18.04.2025
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    companion object: Log()

    @ExceptionHandler(TimeoutException::class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    fun handleTimeoutException() {
        logger.errorM("Timeout occurred while getting delayed response")
    }

}