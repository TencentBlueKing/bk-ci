package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.pojo.coverity.CodeccReport
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelineBuildService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TXUserBuildResourceImpl @Autowired constructor(
    private val buildService: PipelineBuildService,
    private val buildQualityService: PipelineBuildQualityService
) : UserBuildResource {

    override fun manualQualityGateReview(userId: String, projectId: String, pipelineId: String, buildId: String, elementId: String, action: ManualReviewAction): Result<Boolean> {
        checkParam(userId, projectId, pipelineId)
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        if (elementId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
        buildQualityService.buildManualQualityGateReview(userId, projectId, pipelineId, buildId, elementId, action, ChannelCode.BS, ChannelCode.isNeedAuth(ChannelCode.BS))
        return Result(true)
    }

    override fun getCodeccReport(userId: String, projectId: String, pipelineId: String): Result<CodeccReport> {
        checkParam(userId, projectId, pipelineId)
        return Result(buildService.getCodeccReport(userId, projectId, pipelineId))
    }

    private fun checkParam(userId: String, projectId: String, pipelineId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }
}
