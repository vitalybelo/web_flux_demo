package ru.vitos.local.webflux.entity

import org.springframework.data.relational.core.mapping.Column

data class CorrelationIdOnly(
    @Column("correlation_id")
    val correlationId: String
)
