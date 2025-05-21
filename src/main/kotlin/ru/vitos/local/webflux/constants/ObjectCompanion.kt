package ru.vitos.local.webflux.constants

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ObjectCompanion {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ObjectCompanion::class.java)
        const val INVALID_PARAMETERS = "Invalid parameters"
        const val INTERNAL_SERVER_ERROR = "Internal server error. Please check logs for more information."
        const val CORRELATION_ABSENT = "Correlation ID absent in awaiting, insert denied"
    }

}