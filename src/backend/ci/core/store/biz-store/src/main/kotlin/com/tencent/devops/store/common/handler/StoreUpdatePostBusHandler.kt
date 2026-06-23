package com.tencent.devops.store.common.handler

import com.tencent.devops.store.common.service.StoreBaseUpdateService
import com.tencent.devops.store.pojo.common.handler.Handler
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import org.springframework.stereotype.Service

@Service
class StoreUpdatePostBusHandler(
    private val storeBaseUpdateService: StoreBaseUpdateService
) : Handler<StoreUpdateRequest> {

    override fun canExecute(handlerRequest: StoreUpdateRequest): Boolean {
        return true
    }

    override fun execute(handlerRequest: StoreUpdateRequest) {
        // 执行后置业务
        storeBaseUpdateService.handlePostUpdateBus(handlerRequest)
    }
}
