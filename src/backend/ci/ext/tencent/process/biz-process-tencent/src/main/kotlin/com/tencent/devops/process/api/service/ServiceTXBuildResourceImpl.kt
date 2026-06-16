package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.pipeline.IMateBuildStartRequest
import com.tencent.devops.process.service.TxPipelineBuildFacadeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTXBuildResourceImpl @Autowired constructor(
    private val txPipelineBuildFacadeService: TxPipelineBuildFacadeService
) : ServiceTXBuildResource {

    override fun iMateBuildStart(
        userId: String,
        projectId: String,
        pipelineId: String,
        request: IMateBuildStartRequest
    ): Result<BuildId> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        return Result(
            txPipelineBuildFacadeService.iMateBuildStart(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                request = request
            )
        )
    }

    override fun visibilityManualStartupInfo(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Result<BuildManualStartupInfo> {
        checkUserId(userId)
        checkParam(projectId, pipelineId)
        return Result(
            txPipelineBuildFacadeService.visibilityManualStartupInfo(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
    }

    private fun checkParam(projectId: String, pipelineId: String) {
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
