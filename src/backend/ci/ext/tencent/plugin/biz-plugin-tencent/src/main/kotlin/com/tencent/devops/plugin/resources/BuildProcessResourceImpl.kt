package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildProcessResource
import com.tencent.devops.process.api.service.ServiceOperationResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildProcessResourceImpl @Autowired constructor(
    private val client: Client
) : BuildProcessResource {

    override fun getUpdateUser(pipelineId: String): Result<String> {
        return client.get(ServiceOperationResource::class).getUpdateUser(pipelineId)
    }
}