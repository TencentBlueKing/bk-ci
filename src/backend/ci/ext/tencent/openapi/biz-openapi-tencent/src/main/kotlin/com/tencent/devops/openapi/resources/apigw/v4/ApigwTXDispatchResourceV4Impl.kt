package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.devcloud.api.service.ServiceDispatchDcResource
import com.tencent.devops.dispatch.devcloud.pojo.DestroyContainerReq
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXDispatchResourceV4
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXDispatchResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwTXDispatchResourceV4 {
    override fun destroyPersistenceContainer(
        appCode: String?,
        apigwType: String?,
        userId: String,
        destroyContainerReq: DestroyContainerReq
    ): Result<Boolean> {
        return client.get(ServiceDispatchDcResource::class).destroyContainer(userId, destroyContainerReq)
    }
}
