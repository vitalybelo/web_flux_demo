package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.awaitFirstOrNull
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.r2dbc.core.insert
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
     * Предполагается, что запись достоверно существует в таблице
     * @param correlationId идентификатор обратного вызова
     * @return запись из таблицы по correlationId или null
     */
    suspend fun selectCallbackData(correlationId: String): CallbackTable? {

        return r2dbcTemplate
            .select<CallbackTable>()
            .matching(query(where("correlation_id").`is`(correlationId))
                .sort(Sort.by(Sort.Direction.DESC,"timestamp"))
                .limit(1))
            .awaitFirstOrNull()?.let { record ->
                logger.debugM("Received callback data for correlation id = [$correlationId], record = [$record]")
                record
            }
    }


    /**
     * Удаляет все заданные идентификатором callbacks из таблицы callback_table
     * @param correlationId идентификатор запроса
     */
    suspend fun deleteUserInfoCallback(correlationId: String): Long {

        return r2dbcTemplate
            .delete<CallbackTable>()
            .matching(query(where("correlation_id").`is`(correlationId)))
            .all()
            .doOnNext { count ->
                logger.infoM("Callback data deleted for correlation id = $correlationId, count = $count")
            }
            .awaitSingle()
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

        if (!callbackDataAwaitingMap.containsKey(correlationId)) {
            logger.warnM("Skipping callback insert: correlationId = $correlationId, because it not awaited")
            return null
        }
        return try {
            val jsonString = withContext(Dispatchers.Default) {
                objectMapper.writeValueAsString(callbackData)
            }
            val record = CallbackTable(correlationId, callbackType, jsonString)
            val savedRecord = r2dbcTemplate
                .insert<CallbackTable>()
                .using(record)
                .awaitSingle()

            logger.infoM("Successfully inserted data for correlation id = $correlationId :: record = [$record]")
            callbackDataAwaitingMap[correlationId] = true
            savedRecord

        } catch (ex: Exception) {
            logger.errorM("Failed inserting correlation id = $correlationId. message: ${ex.message}, cause: ${ex.cause}")
            null
        }
    }


    suspend fun addAwaiting(id: String) {
        callbackDataAwaitingMap[id] = false
    }


    /**
     * Выполняет удаление ожидаемого id из карты callbackDataAwaitingMap
     * Затем выполняет удаление всех найденных записей в БД с переданным в метод id
     * @param id идентификатор запроса
     */
    suspend fun removeAwaiting(id: String) {

        val removedValue = callbackDataAwaitingMap.remove(id)
        if (removedValue != null) {
            try {
                // Поток приостановится здесь, пока база данных не ответит.
                val count = deleteUserInfoCallback(id)
                logger.infoM("Removed awaiting id = $id, deleted = $count :: map_size = ${callbackDataAwaitingMap.size}")

            } catch (ex: Exception) {
                // id из мапы мы удалили, чтобы не было утечки памяти,
                logger.errorM(
                    "Failed cleaning up DB for id = $id :: ${ex.message}, cause = ${ex.cause}", ex
                )
            }
        } else {
            logger.debugM("Id = $id was not found in awaiting map")
        }
    }

}