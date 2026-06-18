package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.label.PipelineGroupCopyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineLabelResourceImpl @Autowired constructor(
    private val pipelineGroupCopyService: PipelineGroupCopyService
) : OpPipelineLabelResource {

    override fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        labelId: String?
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
        pipelineGroupCopyService.copyAcrossProject(
            userId = userId,
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            labelId = labelId
        )
        return Result(true)
    }
}
