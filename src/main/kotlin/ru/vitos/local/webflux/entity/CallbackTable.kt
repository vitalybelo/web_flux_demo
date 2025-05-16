package ru.vitos.local.webflux.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.lang.System.currentTimeMillis
import java.util.UUID


@Suppress("unused")
@Table(name = "callback_table", schema = "public")
data class CallbackTable(

    @Column("id") @Id val id: UUID,
    @Column("user_id") val userId: String,
    @Column("callback_type") val callbackType: String? = null,
    @Column("callback_json") val callbackJson: String? = null,
    @Column("timestamp") val timestamp: Long? = null,

) {
    constructor() :
            this(UUID.randomUUID(), "", "", "", currentTimeMillis())

    constructor(userId: String,
                callbackType: String,
                callbackJson: String) :
            this(UUID.randomUUID(), userId, callbackType, callbackJson, currentTimeMillis())
}