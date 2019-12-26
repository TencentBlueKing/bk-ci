package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.service.PipelineUserService
import org.springframework.beans.factory.annotation.Autowired

class ServiceOperationResourceImpl @Autowired constructor(
    private val pipelineUserService: PipelineUserService
): ServiceOperationResource {

    override fun getUpdateUser(pipelineId: String): Result<String> {
        val lastUpdateUserMap = pipelineUserService.listUpdateUsers(setOf(pipelineId))
        return Result(lastUpdateUserMap[pipelineId] ?: "")
    }
}