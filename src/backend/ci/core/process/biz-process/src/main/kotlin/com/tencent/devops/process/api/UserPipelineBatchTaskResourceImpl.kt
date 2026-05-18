package com.tencent.devops.process.api

import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserPipelineBatchTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskType
import com.tencent.devops.process.service.PipelineBatchTaskService
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
        page: Int,
        pageSize: Int
    ): Result<SQLPage<PipelineBatchTaskDetailInfo>> {
        return Result(
            pipelineBatchTaskService.listDetails(
                projectId = projectId,
                taskId = taskId,
                page = page,
                pageSize = pageSize
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
