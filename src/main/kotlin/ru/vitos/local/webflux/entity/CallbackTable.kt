package ru.vitos.local.webflux.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.lang.System.currentTimeMillis
import java.util.UUID

@Table(name = "callback_table", schema = "public")
data class CallbackTable(

    @Id
    @Column("id")
    val id: UUID,

    @Column("correlation_id")
    val correlationId: String,

    @Column("callback_type")
    val callbackType: String? = null,

    @Column("callback_json")
    var callbackJson: String? = null,

    @Column("timestamp")
    val timestamp: Long? = null

) {

    constructor(id: String,
                type: String,
                json: String) :
            this(
                UUID.randomUUID(),
                id,
                type,
                json,
                currentTimeMillis())
}