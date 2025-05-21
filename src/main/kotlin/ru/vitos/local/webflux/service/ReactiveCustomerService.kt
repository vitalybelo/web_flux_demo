package ru.vitos.local.webflux.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.vitos.local.webflux.entity.CallbackTable
import ru.vitos.local.webflux.model.CustomerInfo


@Service
class ReactiveCustomerService(

    private val objectMapper: ObjectMapper,
    private val blackBoxService: BlackBoxExternalService
) {


    /**
     * Выполняет реактивный запрос к API сервису "коробки", который в свою очередь возвращает
     * данные по запросу с задержкой до 20 секунд
     * @param userId идентификатор пользователя
     * @return userInfo или ошибку
     */
    fun fetchUserInfoReactive(userId: String): Mono<Any> {

        return Mono.create { sink ->
            blackBoxService.fetchUserInfoData(userId) { result ->
                result
                    .onSuccess { data ->
                        if (data is CallbackTable) {
                            val userInfo =
                                objectMapper.readValue(data.callbackJson, CustomerInfo::class.java)
                            sink.success(userInfo)
                        } else {
                            sink.success(data)
                        }
                    }
                    .onFailure { error ->
                        sink.error(error)
                    }
            }
        }
    }

}