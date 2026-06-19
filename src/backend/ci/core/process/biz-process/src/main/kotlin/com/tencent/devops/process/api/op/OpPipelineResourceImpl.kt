package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.pipeline.PipelineCopyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineResourceImpl @Autowired constructor(
    private val pipelineCopyService: PipelineCopyService
) : OpPipelineResource {

    override fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        pipelineId: String?
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (sourceProjectId.isBlank()) {
            throw ParamBlankException("Invalid sourceProjectId")
        }
        if (targetProjectId.isBlank()) {
            throw ParamBlankException("Invalid targetProjectId")
        }
        pipelineCopyService.copyAcrossProject(
            userId = userId,
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            pipelineId = pipelineId
        )
        return Result(true)
    }

    override fun fixInstanceSetting(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        pipelineId: String
    ): Result<Boolean> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (sourceProjectId.isBlank()) {
            throw ParamBlankException("Invalid sourceProjectId")
        }
        if (targetProjectId.isBlank()) {
            throw ParamBlankException("Invalid targetProjectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        return Result(
            pipelineCopyService.fixInstanceSetting(
                userId = userId,
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                pipelineId = pipelineId
            )
        )
    }
}
