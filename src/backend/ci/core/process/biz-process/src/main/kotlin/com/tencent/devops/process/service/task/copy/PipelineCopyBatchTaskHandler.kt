package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.service.task.PipelineBatchTaskHandler
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyBatchTaskHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineCopyTaskCreateService: PipelineCopyTaskCreateService,
    private val pipelineCopyTaskAnalyzeService: PipelineCopyTaskAnalyzeService,
    private val pipelineCopyTaskExecuteService: PipelineCopyTaskExecuteService,
    private val pipelineCopyTaskRetryService: PipelineCopyTaskRetryService
) : PipelineBatchTaskHandler {

    override fun support(taskType: PipelineBatchTaskType): Boolean {
        return taskType == PipelineBatchTaskType.PIPELINE_COPY
    }

    override fun taskStatusWhenCreate(): PipelineBatchTaskStatus {
        return PipelineBatchTaskStatus.PIPELINE_ANALYZING
    }

    override fun validateWhenCreate(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ) {
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
    }

    override fun detailStatusWhenCreate(): PipelineBatchTaskDetailStatus {
        return PipelineBatchTaskDetailStatus.WAIT_COPY
    }

    override fun handleCreateEvent(event: PipelineBatchTaskCreateEvent) {
        pipelineCopyTaskCreateService.create(event = event)
    }

    override fun handleAnalyzeEvent(event: PipelineBatchTaskAnalyzeEvent) {
        pipelineCopyTaskAnalyzeService.analyze(
            event = event
        )
    }

    override fun validateWhenExecute(userId: String, projectId: String, task: PipelineBatchTask) {
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = param.targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, param.targetProjectId)
            )
        }
        val unProcessedCount = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = task.taskId
        ).filter { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED }
        if (unProcessedCount.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_NOT_ALL_PROCESSED,
                params = arrayOf(task.taskId, unProcessedCount.toString())
            )
        }
    }

    override fun handleExecuteEvent(event: PipelineBatchTaskExecuteEvent) {
        pipelineCopyTaskExecuteService.execute(event = event)
    }

    override fun retry(
        userId: String,
        projectId: String,
        task: PipelineBatchTask
    ) {
        validateProjectManagerWhenRetry(
            userId = userId,
            projectId = projectId,
            task = task
        )
        pipelineCopyTaskRetryService.retry(
            projectId = projectId,
            taskId = task.taskId
        )
    }

    override fun retryPipeline(
        userId: String,
        projectId: String,
        task: PipelineBatchTask,
        pipelineId: String
    ) {
        validateProjectManagerWhenRetry(
            userId = userId,
            projectId = projectId,
            task = task
        )
        pipelineCopyTaskRetryService.retryPipeline(
            projectId = projectId,
            taskId = task.taskId,
            pipelineId = pipelineId
        )
    }

    private fun validateProjectManagerWhenRetry(
        userId: String,
        projectId: String,
        task: PipelineBatchTask
    ) {
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
        val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = param.targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, param.targetProjectId)
            )
        }
    }
}
