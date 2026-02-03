package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.r2dbc.core.insert
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.logging.Log
import java.util.concurrent.ConcurrentHashMap


@Service
@Transactional
@Suppress("DuplicatedCode")
class ReactiveCallbackStore(

    private val r2dbcTemplate: R2dbcEntityTemplate,
    private val objectMapper: ObjectMapper
) {

    companion object: Log()
    private val callbackDataAwaitingMap = ConcurrentHashMap<String, Boolean>()


    /**
     * Метод извлекает запись из таблицы полученных callback по идентификатору запроса
     * Предполагается, что запись достоверно существует в таблице, поскольку при выполнении
     * операции INSERT в таблицу callbacks
     * было проверено scheduler из пост-конструктора
     *
     */
    fun selectCallbackData(correlationId: String): Mono<CallbackTable> {

        val record = r2dbcTemplate.select<CallbackTable>()
            .matching(query(where("correlation_id").`is`(correlationId))
                .sort(Sort.by(Sort.Direction.DESC,"timestamp"))
                .limit(1))
            .first()

        return record.doOnNext { record ->
            logger.debugM("Received callback data for correlation id = [$correlationId], record = [$record]")
        }
    }


    /**
     * Удаляет все заданные идентификатором callbacks из таблицы callback_table
     * @param correlationId идентификатор запроса
     */
    fun deleteUserInfoCallback(correlationId: String) {

        r2dbcTemplate
            .delete<CallbackTable>()
            .matching(query(where("correlation_id").`is`(correlationId)))
            .all()
            .map { count ->
                logger.infoM("Callback data deleted for correlation id = $correlationId, count = $count")
            }
            .subscribe()
    }


    /**
     * Выполняет запись в таблицу callback_table полученного ответа от обратного вызова.
     * Далее, предполагается, что эта запись будет прочитана scheduler-ом и обработана
     *
     * @param correlationId идентификатор запроса
     * @param callbackType тип операции обратного вызова
     * @param callbackData данные полученные от обратного вызова
     * @return сущность сделанной записи в таблицу или null
     */
    suspend fun insertCallbackData(

        correlationId: String,
        callbackType: String,
        callbackData: Any

    ) : CallbackTable? {

        if (callbackDataAwaitingMap.containsKey(correlationId)) {
            val jsonString = objectMapper.writeValueAsString(callbackData)
            val record = CallbackTable(correlationId, callbackType, jsonString)
            try {
                return r2dbcTemplate
                    .insert<CallbackTable>()
                    .using(record)
                    .doOnSuccess { record ->
                        logger.infoM("Successfully inserted data for correlation id = $correlationId :: $callbackType :: record = [$record]")
                        callbackDataAwaitingMap[correlationId] = true
                    }
                    .doOnError { error ->
                        logger.errorM("Failed to insert data for correlation id = $correlationId :: ${error.message}, cause: ${error.cause}")
                    }
                    .awaitSingle()

            } catch (ex: Exception) {
                logger.errorM("Exception of inserting record for correlation id = $correlationId :: ${ex.message}, cause: ${ex.cause}")
            }
        }
        return null
    }


    fun addAwaiting(id: String): Mono<Void> {
        callbackDataAwaitingMap[id] = false
        return Mono.empty()
    }

    fun get(id: String): Mono<Boolean> {
        return Mono.just(callbackDataAwaitingMap[id] == true)
    }

    fun removeAwaiting(id: String): Mono<Void> {

        if (callbackDataAwaitingMap.containsKey(id)) {
            try {
                callbackDataAwaitingMap.remove(id)
                Mono.fromCallable { deleteUserInfoCallback(id) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()

                logger.infoM("Removed awaiting id = $id :: $callbackDataAwaitingMap")
            } catch (ex: Exception) {
                logger.errorM("Failed removing awaiting id = $id :: ${ex.message}, cause: ${ex.cause}")
            }
        }
        return Mono.empty()
    }
}