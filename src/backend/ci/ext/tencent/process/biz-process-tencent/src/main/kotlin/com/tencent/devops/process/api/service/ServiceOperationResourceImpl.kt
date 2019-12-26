package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.service.PipelineUserService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceOperationResourceImpl @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService
) : ServiceOperationResource {

    override fun getUpdateUser(pipelineId: String): Result<String> {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId)
        if(pipelineInfo != null) {
            return Result(pipelineInfo!!.lastModifyUser)
        }
        return Result("")
    }
}