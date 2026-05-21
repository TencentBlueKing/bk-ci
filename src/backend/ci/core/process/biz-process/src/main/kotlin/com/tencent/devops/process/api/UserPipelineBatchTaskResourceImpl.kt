package com.tencent.devops.process.api

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineBatchTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
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
    ): Result<SQLPage<PipelineBatchTaskInfo>> {
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
    ): Result<PipelineBatchTaskInfo?> {
        return Result(pipelineBatchTaskService.get(projectId = projectId, taskId = taskId))
    }

    override fun listDetails(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineName: String?,
        status: PipelineBatchTaskDetailStatus?,
        pac: Boolean?,
        systemAdd: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTaskDetailInfo>> {
        return Result(
            pipelineBatchTaskService.listDetails(
                projectId = projectId,
                taskId = taskId,
                pipelineName = pipelineName,
                status = status,
                pac = pac,
                systemAdd = systemAdd,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun statusSummary(
        userId: String,
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ): Result<List<PipelineBatchTaskStatusSummary>> {
        return Result(
            pipelineBatchTaskService.statusSummary(
                projectId = projectId,
                taskId = taskId,
                taskType = taskType
            )
        )
    }

    override fun config(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineBatchTaskConfigRequest
    ): Result<Boolean> {
        return Result(
            pipelineBatchTaskService.config(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                request = request
            )
        )
    }

    override fun excludePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Result<Boolean> {
        return Result(
            pipelineBatchTaskService.excludePipeline(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId
            )
        )
    }

    override fun restorePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Result<Boolean> {
        return Result(
            pipelineBatchTaskService.restorePipeline(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId
            )
        )
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
}
