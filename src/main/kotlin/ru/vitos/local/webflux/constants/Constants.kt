package ru.vitos.local.webflux.constants

import ru.vitos.local.webflux.logging.Log

class Constants {

    companion object: Log() {

        const val INVALID_PARAMETERS = "Invalid parameters"
        const val INTERNAL_SERVER_ERROR = "Internal server error. Please check logs for more information."
        const val CORRELATION_ABSENT = "Correlation ID absent in awaiting, insert denied"
    }

}