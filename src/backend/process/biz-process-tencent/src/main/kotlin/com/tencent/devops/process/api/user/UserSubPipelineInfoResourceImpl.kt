package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.SubPipelineStartUpInfo
import com.tencent.devops.process.service.SubPipelineStartUpService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserSubPipelineInfoResourceImpl @Autowired constructor (
    private val subPipeService: SubPipelineStartUpService
) : UserSubPipelineInfoResource {

    override fun subpipManualStartupInfo(userId: String, projectId: String, pipelineId: String): Result<List<SubPipelineStartUpInfo>> {
        checkParam(userId, projectId)
        return subPipeService.subpipManualStartupInfo(userId, projectId, pipelineId)
    }

    private fun checkParam(userId: String, projectId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}