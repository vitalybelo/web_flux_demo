package ru.vitos.local.webflux.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.vitos.local.webflux.model.CustomerInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Класс хранилище данных callback для обработки запросов
 * @author Vitalii Belotserkovskii, 14.05.2025
 */
@Service
@Suppress("unused")
class CallbackDataService {


    private var requestMap: MutableMap<String, CustomerInfo> = ConcurrentHashMap()

    companion object {
        val log: Logger = LoggerFactory.getLogger(CallbackDataService::class.java)
    }


    /**
     * Метод проверяет наличие в хранилище записи для конкретного идентификатора пользователя.
     * Если запись по идентификатору существует, считывается значение по ключу и запись удаляется.
     *
     * @param userId идентификатор пользователя
     * @return имя пользователя или null, если в хранилище нет записи для идентификатора
     */
    fun getCustomerByRequestId(userId: String?): CustomerInfo? {

        if (userId != null && requestMap.containsKey(userId)) {
            val userInfo = requestMap[userId]
            requestMap.remove(userId)
            return userInfo
        }
        return null
    }

    /**
     * Добавляет запись в хранилище для заданного идентификатора.
     *
     * @param userId идентификатор пользователя
     * @param userInfo информация о пользователе
     */
    fun addCustomerInfoRequest(userId: String, userInfo: CustomerInfo) {

        if (requestMap.containsKey(userId)) requestMap.remove(userId)
        requestMap[userId] = userInfo
    }

    /**
     * @return карту хранилища
     */
    fun getRequestMap(): Map<String, CustomerInfo> {
        return requestMap
    }

}