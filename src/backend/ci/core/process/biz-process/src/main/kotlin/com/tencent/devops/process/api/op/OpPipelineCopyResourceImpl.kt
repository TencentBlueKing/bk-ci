package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.FixPipelineSubPipelineProjectRequest
import com.tencent.devops.process.pojo.pipeline.FixTemplateSubPipelineProjectRequest
import com.tencent.devops.process.service.pipeline.PipelineCopyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineCopyResourceImpl @Autowired constructor(
    private val pipelineCopyService: PipelineCopyService
) : OpPipelineCopyResource {

    override fun fixInstanceSetting(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourcePipelineId: String,
        targetPipelineId: String?
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
        if (sourcePipelineId.isBlank()) {
            throw ParamBlankException("Invalid sourcePipelineId")
        }
        val resolvedTargetPipelineId = targetPipelineId?.takeIf { it.isNotBlank() } ?: sourcePipelineId
        return Result(
            pipelineCopyService.fixInstanceSetting(
                userId = userId,
                sourceProjectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourcePipelineId = sourcePipelineId,
                targetPipelineId = resolvedTargetPipelineId
            )
        )
    }

    override fun copyLabelsAcrossProject(
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
        pipelineCopyService.copyLabelsAcrossProject(
            userId = userId,
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            labelId = labelId
        )
        return Result(true)
    }

    override fun copyViewsAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        viewName: String?
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
        pipelineCopyService.copyViewsAcrossProject(
            userId = userId,
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            viewName = viewName
        )
        return Result(true)
    }

    override fun fixPipelineSubPipelineProject(
        userId: String,
        projectId: String,
        request: FixPipelineSubPipelineProjectRequest
    ): Result<Boolean> {
        validateSubPipelineProjectFixRequest(
            userId = userId,
            projectId = projectId,
            resourceIds = request.pipelineIds,
            sourceSubProjectId = request.sourceSubProjectId,
            targetSubProjectId = request.targetSubProjectId,
            resourceIdsFieldName = "pipelineIds"
        )
        pipelineCopyService.fixPipelineSubPipelineProject(
            userId = userId,
            projectId = projectId,
            pipelineIds = request.pipelineIds,
            sourceSubProjectId = request.sourceSubProjectId,
            targetSubProjectId = request.targetSubProjectId
        )
        return Result(true)
    }

    override fun fixTemplateSubPipelineProject(
        userId: String,
        projectId: String,
        request: FixTemplateSubPipelineProjectRequest
    ): Result<Boolean> {
        validateSubPipelineProjectFixRequest(
            userId = userId,
            projectId = projectId,
            resourceIds = request.templateIds,
            sourceSubProjectId = request.sourceSubProjectId,
            targetSubProjectId = request.targetSubProjectId,
            resourceIdsFieldName = "templateIds"
        )
        pipelineCopyService.fixTemplateSubPipelineProject(
            projectId = projectId,
            templateIds = request.templateIds,
            sourceSubProjectId = request.sourceSubProjectId,
            targetSubProjectId = request.targetSubProjectId
        )
        return Result(true)
    }

    private fun validateSubPipelineProjectFixRequest(
        userId: String,
        projectId: String,
        resourceIds: List<String>,
        sourceSubProjectId: String,
        targetSubProjectId: String,
        resourceIdsFieldName: String
    ) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (resourceIds.isEmpty()) {
            throw ParamBlankException("Invalid $resourceIdsFieldName")
        }
        if (sourceSubProjectId.isBlank()) {
            throw ParamBlankException("Invalid sourceSubProjectId")
        }
        if (targetSubProjectId.isBlank()) {
            throw ParamBlankException("Invalid targetSubProjectId")
        }
    }
}
