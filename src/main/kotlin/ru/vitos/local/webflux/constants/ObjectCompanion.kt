package ru.vitos.local.webflux.constants

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ObjectCompanion {

    companion object {
        val log: Logger = LoggerFactory.getLogger(ObjectCompanion::class.java)
        const val INVALID_PARAMETERS = "Invalid parameters"
    }

}