package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.logging.Log
import ru.vitos.local.webflux.model.CustomerInfo


@Service
class ReactiveCustomerService(

    private val objectMapper: ObjectMapper,
    private val blackBoxService: BlackBoxExternalService
) {

    companion object: Log()

    /**
     * Выполняет реактивный запрос к API сервису "коробки", который в свою очередь возвращает
     * данные по запросу с задержкой до 20 секунд
     *
     * @param userId идентификатор пользователя
     * @return userInfo или ошибку
     */
    fun fetchUserInfoReactive(userId: String): Mono<CustomerInfo> {

        logger.debugM("Start fetch user info from Black Box Service for user = $userId")
        // Используем билдер mono, чтобы запустить корутину и вернуть результат как Mono
        return mono {
            try {
                val data = blackBoxService.fetchUserInfoData(userId)
                val userInfo =
                    objectMapper.readValue(data?.callbackJson, CustomerInfo::class.java)
                userInfo

            } catch (ex: Exception) {
                logger.errorM(
                    "Error processing user info for user = $userId, message = ${ex.message}, cause = ${ex.cause}"
                )
                null
            }
        }
    }
}