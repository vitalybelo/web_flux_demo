package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.constants.ObjectCompanion.Companion.log
import java.util.concurrent.ConcurrentHashMap


@Service
@Transactional
@Suppress("DuplicatedCode")
class ReactiveCallbackStore(

    private val r2dbcTemplate: R2dbcEntityTemplate,
    private val objectMapper: ObjectMapper
) {

    private val callbackDataAwaitingMap = ConcurrentHashMap<String, Boolean?>()
    /*
    private lateinit var scheduler: ScheduledExecutorService

    /**
     * Инициализируем scheduler для сбора информации
     */
    @PostConstruct
    fun init() {
        scheduler = Executors.newSingleThreadScheduledExecutor()
        log.info("ReactiveCallbackStore :: Запуск ScheduledExecutorService...")

        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate( {

            scanCallbackDataTable()
            log.info("ReactiveCallbackStore :: {}", callbackDataAwaitingMap)

        }, 1L, 2L, TimeUnit.SECONDS)
    }

    @PreDestroy
    fun cleanup() {
        scheduler.shutdown()
        println("BackgroundDataUpdater: ScheduledExecutorService завершен.")
        // Ожидаем завершения задач (не обязательно, но хорошая практика)
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }


    /**
     * Метод извлекает из таблицы полученных callback запросов - записи по списку correlationId.
     * Если запись по ожидаемому correlationId будет найдена, это означает что callback по запросу
     * с этим correlationId поступил и его можно обработать. Для этого проставляется значение true
     * в карте callbackDataAwaitingMap - далее, ожидающий процесс извлекает и удаляет запись
     */
    fun scanCallbackDataTable() {

        val correlationIdList = callbackDataAwaitingMap.keys.toList()
        if (correlationIdList.isNotEmpty()) {

            val recordList = r2dbcTemplate
                .select(CallbackTable::class.java)
                .matching(query(where("correlation_id").`in`(correlationIdList)))
                .all().collectList()

            recordList.map { list ->
                list.map { mapper -> mapper.correlationId }.forEach { id ->
                    callbackDataAwaitingMap[id] = true
                }
            }.subscribe()
        }
    }
    */


    /**
     * Метод извлекает запись из таблицы полученных callback по идентификатору запроса
     * Предполагается, что запись достоверно существует в таблице, поскольку при выполнении
     * операции INSERT в таблицу callbacks
     * было проверено scheduler из пост-конструктора
     *
     */
    fun selectCallbackData(correlationId: String): Mono<CallbackTable?> {

        val record = r2dbcTemplate
            .select(CallbackTable::class.java)
            .matching(query(where("correlation_id").`is`(correlationId))
                .sort(Sort.by(Sort.Direction.DESC,"timestamp"))
                .limit(1))
            .first()
        return record
    }


    /**
     * Удаляет все заданные идентификатором callbacks из таблицы callback_table
     * @param correlationId идентификатор запроса
     */
    fun deleteUserInfoCallback(correlationId: String) {

        r2dbcTemplate
            .delete(CallbackTable::class.java)
            .matching(query(where("correlation_id").`is`(correlationId)))
            .all()
            .map { count ->
                log.info("Callback data deleted for $correlationId with size = $count")
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
                    .insert(CallbackTable::class.java)
                    .using(record)
                    .doOnSuccess { record ->
                        log.info("Successfully inserted callback data for $correlationId :: $callbackType")
                        callbackDataAwaitingMap[correlationId] = true
                    }
                    .doOnError { error -> log.error("Failed to insert callback data for $correlationId :: ${error.message}") }
                    .awaitSingle()

            } catch (e: Exception) {
                log.error(">>>> Exception happen during inserting :: ${e.message}")
            }
        }
        return null
    }


    fun addAwaiting(id: String): Mono<Void> {
        callbackDataAwaitingMap[id] = false
        return Mono.empty()
    }

    fun get(id: String): Mono<Boolean> {
        return Mono.just(callbackDataAwaitingMap.get(id) == true)
    }

    fun removeAwaiting(id: String): Mono<Void> {

        if (callbackDataAwaitingMap.containsKey(id)) {
            try {
                callbackDataAwaitingMap.remove(id)
                Mono.fromCallable { deleteUserInfoCallback(id) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe()

                log.info("Removed awaiting $id :: $callbackDataAwaitingMap")
            } catch (e: Exception) {
                log.error(">>> Exception happen during removing awaiting :: ${e.message}")
            }
        }
        return Mono.empty()
    }


}