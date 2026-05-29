package com.tencent.devops.process.api

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineBatchTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.service.task.PipelineBatchTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserPipelineBatchTaskResourceImpl @Autowired constructor(
    private val pipelineBatchTaskService: PipelineBatchTaskService
) : UserPipelineBatchTaskResource {

    override fun list(
        userId: String,
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTask>> {
        return Result(
            pipelineBatchTaskService.list(
                projectId = projectId,
                type = type,
                status = status,
                creator = creator,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun create(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ): Result<String> {
        return Result(pipelineBatchTaskService.create(userId = userId, projectId = projectId, request = request))
    }

    override fun get(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<PipelineBatchTask?> {
        return Result(pipelineBatchTaskService.get(projectId = projectId, taskId = taskId))
    }

    override fun listDetails(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineName: String?,
        status: PipelineBatchTaskDetailStatus?,
        pac: Boolean?,
        subPipeline: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTaskDetail>> {
        return Result(
            pipelineBatchTaskService.listDetails(
                projectId = projectId,
                taskId = taskId,
                pipelineName = pipelineName,
                status = status,
                pac = pac,
                subPipeline = subPipeline,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun detailStatusSummary(
        userId: String,
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ): Result<List<PipelineBatchTaskDetailStatusSummary>> {
        return Result(
            pipelineBatchTaskService.detailStatusSummary(
                projectId = projectId,
                taskId = taskId,
                taskType = taskType
            )
        )
    }

    override fun excludePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Result<Boolean> {
        pipelineBatchTaskService.excludePipeline(
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId
        )
        return Result(true)
    }

    override fun restorePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Result<Boolean> {
        pipelineBatchTaskService.restorePipeline(
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId
        )
        return Result(true)
    }

    override fun restoreAllPipelines(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<Boolean> {
        pipelineBatchTaskService.restoreAllPipelines(
            projectId = projectId,
            taskId = taskId
        )
        return Result(true)
    }

    override fun execute(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<Boolean> {
        pipelineBatchTaskService.execute(
            userId = userId,
            projectId = projectId,
            taskId = taskId
        )
        return Result(true)
    }

    override fun delete(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<Boolean> {
        return Result(
            pipelineBatchTaskService.delete(
                userId = userId,
                projectId = projectId,
                taskId = taskId
            )
        )
    }

    override fun retry(
        userId: String,
        projectId: String,
        taskId: String
    ): Result<Boolean> {
        return Result(pipelineBatchTaskService.retry(projectId = projectId, taskId = taskId))
    }

    override fun retryPipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
