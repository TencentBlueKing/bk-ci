package com.tencent.devops.store.common.handler

import com.tencent.devops.store.common.service.StoreBaseCreateService
import com.tencent.devops.store.pojo.common.handler.Handler
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import org.springframework.stereotype.Service

@Service
class StoreCreatePostBusHandler(
    private val storeBaseCreateService: StoreBaseCreateService
) : Handler<StoreCreateRequest> {

    override fun canExecute(handlerRequest: StoreCreateRequest): Boolean {
        return true
    }

    override fun execute(handlerRequest: StoreCreateRequest) {
        // 执行后置业务
        storeBaseCreateService.handlePostCreateBus(handlerRequest)
    }
}
