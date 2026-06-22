package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.view.PipelineViewCopyService
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.yaml.PipelineYamlViewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineViewResourceImpl @Autowired constructor(
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val pipelineViewCopyService: PipelineViewCopyService
) : OpPipelineViewResource {
    override fun initAllView(userId: String): Result<Boolean> {
        Thread { pipelineViewGroupService.initAllView() }.start()
        return Result(true)
    }

    override fun deleteYamlView(
        projectId: String,
        repoHashId: String,
        directory: String
    ): Result<Boolean> {
        pipelineYamlViewService.deleteYamlView(
            projectId = projectId,
            repoHashId = repoHashId,
            directory = directory
        )
        return Result(true)
    }

    override fun copyAcrossProject(
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
        pipelineViewCopyService.copyAcrossProject(
            userId = userId,
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            viewName = viewName
        )
        return Result(true)
    }
}
