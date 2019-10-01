package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.codecc.api.UserBuildCodeccResource
import com.tencent.devops.plugin.codecc.pojo.coverity.CodeccReport
import com.tencent.devops.plugin.codecc.service.PipelineCodeccService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserBuildCodeccResourceImpl @Autowired constructor(private val pipelineCodeccService: PipelineCodeccService) :
    UserBuildCodeccResource {

    override fun getCodeccReport(userId: String, projectId: String, pipelineId: String): Result<CodeccReport> {
        checkParam(userId, projectId, pipelineId)
        return Result(pipelineCodeccService.getCodeccReport(userId, projectId, pipelineId))
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String) {
        val message = when {
            userId.isBlank() -> "Invalid userId"
            pipelineId.isBlank() -> "Invalid pipelineId"
            projectId.isBlank() -> "Invalid projectId"
            else -> null
        }

        if (message.isNullOrBlank()) {
            return
        }

        throw ParamBlankException("Invalid userId")
    }
}
