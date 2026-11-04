package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCleanupReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewCreateReq
import com.tencent.devops.process.pojo.pipeline.PipelineYamlViewGroupUpdateReq
import com.tencent.devops.process.service.view.PipelineViewGroupService
import com.tencent.devops.process.yaml.PipelineYamlViewService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServicePipelineYamlViewResourceImpl @Autowired constructor(
    private val pipelineYamlViewService: PipelineYamlViewService,
    private val pipelineViewGroupService: PipelineViewGroupService
) : ServicePipelineYamlViewResource {

    override fun createYamlViewIfAbsent(
        userId: String,
        projectId: String,
        request: PipelineYamlViewCreateReq
    ): Result<Boolean> {
        pipelineYamlViewService.createYamlViewIfAbsent(
            userId = userId,
            projectId = projectId,
            repoHashId = request.repoHashId,
            aliasName = request.aliasName,
            directoryList = request.directoryList
        )
        return Result(true)
    }

    override fun cleanupYamlView(
        userId: String,
        projectId: String,
        request: PipelineYamlViewCleanupReq
    ): Result<Boolean> {
        pipelineYamlViewService.cleanupYamlView(
            userId = userId,
            projectId = projectId,
            repoHashId = request.repoHashId,
            directory = request.directory,
            pipelineId = request.pipelineId
        )
        return Result(true)
    }

    override fun updateGroupAfterPipelineUpdate(
        userId: String,
        projectId: String,
        request: PipelineYamlViewGroupUpdateReq
    ): Result<Boolean> {
        pipelineViewGroupService.updateGroupAfterPipelineUpdate(
            projectId = projectId,
            pipelineId = request.pipelineId,
            pipelineName = request.pipelineName,
            creator = request.creator,
            userId = userId
        )
        return Result(true)
    }
}
