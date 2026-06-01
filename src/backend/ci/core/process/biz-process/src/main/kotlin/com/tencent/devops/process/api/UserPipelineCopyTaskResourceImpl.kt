package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyPipelineInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceGroup
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteProgress
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskExecuteSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.service.task.PipelineCopyTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineCopyTaskResourceImpl @Autowired constructor(
    private val pipelineCopyTaskService: PipelineCopyTaskService
) : UserPipelineCopyTaskResource {

    override fun get(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<PipelineCopyTask?> {
        return Result(pipelineCopyTaskService.get(userId = userId, projectId = projectId, taskId = taskId))
    }

    override fun saveConfigDraft(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskConfigRequest
    ): Result<Boolean> {
        pipelineCopyTaskService.saveConfigDraft(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            request = request
        )
        return Result(true)
    }

    override fun analyzeResourceDepend(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskConfigRequest
    ): Result<Boolean> {
        return Result(
            pipelineCopyTaskService.analyzeResourceDepend(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                request = request
            )
        )
    }

    override fun listResource(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType?,
        resourceName: String?,
        copyAction: PipelineCopyAction?
    ): Result<List<PipelineCopyResourceGroup>> {
        return Result(
            pipelineCopyTaskService.listResource(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                resourceType = resourceType,
                resourceName = resourceName,
                copyAction = copyAction
            )
        )
    }

    override fun listResourcePipelines(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        pipelineName: String?
    ): Result<List<PipelineCopyPipelineInfo>> {
        return Result(
            pipelineCopyTaskService.listResourcePipelines(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                resourceType = resourceType,
                resourceId = resourceId,
                pipelineName = pipelineName
            )
        )
    }

    override fun saveResourceDraft(
        userId: String,
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ): Result<Boolean> {
        pipelineCopyTaskService.saveResourceDraft(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            resources = resources
        )
        return Result(true)
    }

    override fun prepareExecute(
        userId: String,
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ): Result<Boolean> {
        pipelineCopyTaskService.prepareExecute(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            resources = resources
        )
        return Result(true)
    }

    override fun execute(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<Boolean> {
        pipelineCopyTaskService.execute(
            userId = userId,
            projectId = projectId,
            taskId = taskId
        )
        return Result(true)
    }

    override fun executeSummary(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<PipelineCopyTaskExecuteSummary> {
        return Result(
            pipelineCopyTaskService.executeSummary(
                projectId = projectId,
                taskId = taskId
            )
        )
    }

    override fun executeProgress(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<PipelineCopyTaskExecuteProgress> {
        return Result(
            pipelineCopyTaskService.executeProgress(
                projectId = projectId,
                taskId = taskId
            )
        )
    }

    override fun confirmResource(
        userId: String,
        projectId: String,
        taskId: String,
        resourceType: PipelineDependentResourceType,
        resourceId: String,
        confirmed: Boolean
    ): Result<Boolean> {
        return Result(
            pipelineCopyTaskService.confirmResource(
                projectId = projectId,
                taskId = taskId,
                resourceType = resourceType,
                resourceId = resourceId,
                confirmed = confirmed
            )
        )
    }
}
