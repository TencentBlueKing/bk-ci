package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ServiceItemRelResource
import com.tencent.devops.store.service.ExtItemServiceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceItemRelResourceImpl @Autowired constructor(
    private val extServiceItemService: ExtItemServiceService
) : ServiceItemRelResource {

    override fun updateItemService(userId: String, itemId: String, serviceId: String): Result<Boolean> {
        return extServiceItemService.updateItemService(userId, itemId, serviceId)
    }
}