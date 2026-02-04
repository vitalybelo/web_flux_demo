package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.r2dbc.core.insert
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Service
import ru.vitos.local.webflux.constants.CallbackTypes
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo

/**
 * Класс хранилище данных callback для обработки запросов
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
@Suppress("unused")
class CallbackDataService(

    private val r2dbcEntityTemplate: R2dbcEntityTemplate,
    private val objectMapper: ObjectMapper
) {

    companion object: Log()

    /**
     * Метод проверяет наличие в хранилище записи для пользователя, заданного идентификатором,
     * с признаком USER_INFO. Если запись или несколько существуют, считывается последняя, затем
     * удаляются все записи для заданного пользователя с признаком USER_INFO
     *
     * @param correlationId идентификатор запроса
     * @return имя пользователя или null, если в хранилище нет записи для идентификатора
     */
    suspend fun fetchUserInfoCallback(correlationId: String): CustomerInfo? {

        return findUserInfoCallback(correlationId)?.let { userInfo ->
            deleteUserInfoCallback(correlationId)
            userInfo
        }
    }


    /**
     * Удаляет для пользователя заданного идентификатором все callbacks, которые имеют признак USER_INFO
     * @param correlationId идентификатор запроса
     */
    suspend fun deleteUserInfoCallback(correlationId: String) {

        r2dbcEntityTemplate
            .delete<CallbackTable>()
            .matching(
                query(
                    where("correlation_id").`is`(correlationId)
                        .and("callback_type").`is`(CallbackTypes.USER_INFO.name)))
            .all()
            .awaitSingleOrNull()?.let { count ->
                logger.infoM("User info successfully deleted for correlationId = $correlationId :: count = $count")
            }
    }


    /**
     * Ищет в хранилище запись - которая делается в таблице при получении callback
     * @param correlationId идентификатор запроса
     * @return найденную запись или null
     */
    suspend fun findUserInfoCallback(correlationId: String): CustomerInfo? {

        val record = r2dbcEntityTemplate
            .select<CallbackTable>()
            .matching(query(where("correlation_id").`is`(correlationId)
                    .and("callback_type").`is`(CallbackTypes.USER_INFO.name))
                    .sort(Sort.by(Sort.Direction.DESC,"timestamp"))
                    .limit(1))
            .all()
            .awaitFirstOrNull() ?: return null
            return try {
                val userInfo =
                    objectMapper.readValue(record.callbackJson, CustomerInfo::class.java)
                logger.infoM("User info found for correlationId = $correlationId. UserInfo: $userInfo")
                userInfo

            } catch (ex: Exception) {
                logger.error("""Failed to parse callback JSON 
                    | table record = $record
                    | message = ${ex.message}
                    | cause = ${ex.cause}
                    """.trimMargin()
                )
                null
            }
    }


    /**
     * Добавляет запись в хранилище для заданного идентификатора user_id - о полученной информации
     * пользователя от стороннего сервиса callback-ом.
     *
     * @param correlationId идентификатор запроса
     * @param userInfo информация о пользователе
     */
    suspend fun addUserInfoCallback(
        correlationId: String,
        userInfo: CustomerInfo

    ): CallbackTable? {

        return try {
            val userInfoJsonString = objectMapper.writeValueAsString(userInfo)
            val record = CallbackTable(
                correlationId,
                CallbackTypes.USER_INFO.name,
                userInfoJsonString
            )
            r2dbcEntityTemplate
                .insert<CallbackTable>()
                .using(record)
                .awaitSingle()
                .also { record ->
                    logger.debugM("Callback added to table :: result = $record")
                }
        } catch (ex: Exception) {
            logger.errorM("Failed to add user info to table :: message = ${ex.message}, cause = ${ex.cause}")
            null
        }
    }

}