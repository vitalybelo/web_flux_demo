package ru.vitos.local.webflux.repository

import org.springframework.data.repository.reactive.ReactiveCrudRepository
import ru.vitos.local.webflux.entity.CallbackTable

interface CallBackRepository : ReactiveCrudRepository<CallbackTable, String>