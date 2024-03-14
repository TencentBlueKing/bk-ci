package com.tencent.devops.openapi.resources.apigw.v4.job

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.job.ServiceJobResource
import com.tencent.devops.openapi.api.apigw.v4.job.ApigwJobResourceV4
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwJobResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwJobResourceV4 {
    override fun writeDisplayName(userId: String) {
        client.get(ServiceJobResource::class).writeDisplayName(userId)
    }

    override fun updateDevopsAgent(userId: String) {
        client.get(ServiceJobResource::class).updateDevopsAgent(userId)
    }
}
