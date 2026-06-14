package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatusSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailVo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBatchTaskService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val handlers: List<PipelineBatchTaskHandler>,
    private val pipelineBatchTaskFactory: PipelineBatchTaskFactory
) {

    fun list(
        projectId: String,
        type: PipelineBatchTaskType?,
        status: PipelineBatchTaskStatus?,
        creator: String?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineBatchTask> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val count = pipelineBatchTaskDao.count(
            dslContext = dslContext,
            projectId = projectId,
            type = type,
            status = status,
            creator = creator
        )
        val records = pipelineBatchTaskDao.list(
            dslContext = dslContext,
            projectId = projectId,
            type = type,
            status = status,
            creator = creator,
            offset = offset,
            limit = limit
        )
        return SQLPage(count = count, records = records)
    }

    fun count(projectId: String, status: PipelineBatchTaskStatus?): Long {
        return pipelineBatchTaskDao.count(
            dslContext = dslContext,
            projectId = projectId,
            type = null,
            status = status,
            creator = null
        )
    }

    fun create(
        userId: String,
        projectId: String,
        request: PipelineBatchTaskCreateRequest
    ): String {
        validateCreateRequest(request)
        val handler = getHandler(taskType = request.taskType)
        handler.validateWhenCreate(
            userId = userId,
            projectId = projectId,
            request = request
        )
        val taskId = UUIDUtil.generate()
        val details = pipelineBatchTaskFactory.buildBatchTaskDetail(
            projectId = projectId,
            taskId = taskId,
            taskType = request.taskType,
            status = handler.detailStatusWhenCreate(),
            pipelineIds = request.pipelineIds
        )
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBatchTaskDao.create(
                dslContext = transactionContext,
                taskId = taskId,
                projectId = projectId,
                taskName = request.taskName,
                taskType = request.taskType,
                taskParam = null,
                status = handler.taskStatusWhenCreate(),
                step = PipelineBatchTaskStep.CONFIG,
                totalCount = details.size,
                subPipelineCount = details.count { it.subPipeline },
                pacCount = details.count { it.pac },
                creator = userId
            )
            pipelineBatchTaskDetailDao.batchCreate(
                dslContext = transactionContext,
                details = details
            )
        }
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskCreateEvent(
                taskId = taskId,
                taskType = request.taskType,
                projectId = projectId
            )
        )
        return taskId
    }

    fun get(projectId: String, taskId: String): PipelineBatchTask? {
        return pipelineBatchTaskDao.get(dslContext = dslContext, projectId = projectId, taskId = taskId)
    }

    fun listDetails(
        projectId: String,
        taskId: String,
        pipelineName: String?,
        pipelineCreator: String?,
        status: PipelineBatchTaskDetailStatus?,
        pac: Boolean?,
        subPipeline: Boolean?,
        page: Int,
        pageSize: Int
    ): SQLPage<PipelineBatchTaskDetailVo> {
        val (offset, limit) = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        val count = pipelineBatchTaskDetailDao.count(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineName = pipelineName,
            pipelineCreator = pipelineCreator,
            status = status,
            pac = pac,
            subPipeline = subPipeline
        )
        val records = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineName = pipelineName,
            pipelineCreator = pipelineCreator,
            status = status,
            pac = pac,
            subPipeline = subPipeline,
            offset = offset,
            limit = limit
        ).map {
            PipelineBatchTaskDetailVo(it)
        }
        return SQLPage(count = count, records = records)
    }

    fun detailStatusSummary(
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType
    ): List<PipelineBatchTaskDetailStatusSummary> {
        return pipelineBatchTaskDetailDao.detailStatusSummary(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            taskType = taskType
        )
    }

    fun excludePipeline(
        projectId: String,
        taskId: String,
        pipelineId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_EXCLUDE,
                params = arrayOf(pipelineId, task.status.name)
            )
        }
        val detail = getTaskDetail(projectId = projectId, taskId = taskId, pipelineId = pipelineId)
        if (detail.subPipeline || detail.status in DETAIL_STATUS_CAN_NOT_EXCLUDE) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_EXCLUDE,
                params = arrayOf(pipelineId, detail.status.name)
            )
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = setOf(pipelineId),
            status = PipelineBatchTaskDetailStatus.EXCLUDED,
            change = true
        )
    }

    fun restorePipeline(
        projectId: String,
        taskId: String,
        pipelineId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_RESTORE,
                params = arrayOf(pipelineId, task.status.name)
            )
        }
        val detail = getTaskDetail(projectId = projectId, taskId = taskId, pipelineId = pipelineId)
        if (detail.status != PipelineBatchTaskDetailStatus.EXCLUDED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_RESTORE,
                params = arrayOf(pipelineId, detail.status.name)
            )
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = setOf(pipelineId),
            status = getHandler(detail.taskType).detailStatusWhenCreate(),
            change = true
        )
    }

    fun restoreAllPipelines(
        projectId: String,
        taskId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        val pipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineBatchTaskDetailStatus.EXCLUDED,
            subPipeline = false
        ).map { it.pipelineId }.toSet()
        if (pipelineIds.isEmpty()) {
            return
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = pipelineIds,
            status = getHandler(task.taskType).detailStatusWhenCreate(),
            change = true
        )
    }

    fun execute(
        userId: String,
        projectId: String,
        taskId: String
    ) {
        val task = getTask(projectId = projectId, taskId = taskId)
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_EXECUTE,
                params = arrayOf(taskId, task.status.name)
            )
        }
        val handler = getHandler(taskType = task.taskType)
        handler.validateWhenExecute(
            userId = userId,
            projectId = projectId,
            task = task
        )
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
                taskId = task.taskId,
                taskType = task.taskType,
                projectId = task.projectId
            )
        )
    }

    fun delete(
        userId: String,
        projectId: String,
        taskId: String
    ): Boolean {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        if (task.status != PipelineBatchTaskStatus.DRAFT) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_DELETE,
                params = arrayOf(taskId, task.status.name)
            )
        }
        getHandler(taskType = task.taskType).validateWhenDelete(
            userId = userId,
            projectId = projectId,
            task = task
        )
        val deletedTaskName = task.taskName?.takeIf { it.isNotBlank() }?.let(::buildDeletedTaskName)
        return pipelineBatchTaskDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskUpdate(
                projectId = projectId,
                taskId = taskId,
                taskName = deletedTaskName,
                status = PipelineBatchTaskStatus.DELETED
            )
        ) == 1
    }

    fun retry(
        userId: String,
        projectId: String,
        taskId: String
    ): Boolean {
        val task = getTask(projectId = projectId, taskId = taskId)
        validateRetryableTaskStatus(task = task)
        getHandler(taskType = task.taskType).retry(
            userId = userId,
            projectId = projectId,
            task = task
        )
        return true
    }

    fun retryPipeline(
        userId: String,
        projectId: String,
        taskId: String,
        pipelineId: String
    ): Boolean {
        val task = getTask(projectId = projectId, taskId = taskId)
        validateRetryableTaskStatus(task = task)
        val detail = getTaskDetail(
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId
        )
        if (detail.status != PipelineBatchTaskDetailStatus.FAILED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_RETRY,
                params = arrayOf(pipelineId, detail.status.name)
            )
        }
        getHandler(taskType = task.taskType).retryPipeline(
            userId = userId,
            projectId = projectId,
            task = task,
            pipelineId = pipelineId
        )
        return true
    }

    private fun validateRetryableTaskStatus(task: PipelineBatchTask) {
        if (task.status !in RETRYABLE_TASK_STATUSES) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_RETRY,
                params = arrayOf(task.taskId, task.status.name)
            )
        }
    }

    private fun validateCreateRequest(request: PipelineBatchTaskCreateRequest) {
        if (request.pipelineIds.isEmpty()) {
            throw InvalidParamException("pipelineIds cannot be empty")
        }
    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTask {
        return pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
    }

    private fun getTaskDetail(
        projectId: String,
        taskId: String,
        pipelineId: String
    ): PipelineBatchTaskDetail {
        return pipelineBatchTaskDetailDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
    }

    private fun getHandler(taskType: PipelineBatchTaskType): PipelineBatchTaskHandler {
        return handlers.singleOrNull { it.support(taskType) }
            ?: throw InvalidParamException("unsupported taskType: $taskType")
    }

    private fun buildDeletedTaskName(taskName: String): String {
        val timestamp = System.currentTimeMillis()
        val suffix = "_$timestamp"
        val maxPrefixLength = TASK_NAME_MAX_LENGTH - suffix.length
        val prefix = if (taskName.length > maxPrefixLength) {
            taskName.substring(0, maxPrefixLength)
        } else {
            taskName
        }
        return "$prefix$suffix"
    }

    companion object {
        private const val TASK_NAME_MAX_LENGTH = 128

        private val DETAIL_STATUS_CAN_NOT_EXCLUDE = setOf(
            PipelineBatchTaskDetailStatus.EXCLUDED,
            PipelineBatchTaskDetailStatus.EXECUTING,
            PipelineBatchTaskDetailStatus.SUCCESS,
            PipelineBatchTaskDetailStatus.FAILED
        )

        private val RETRYABLE_TASK_STATUSES = setOf(
            PipelineBatchTaskStatus.FAILED,
            PipelineBatchTaskStatus.PARTIAL_FAILED
        )
    }
}
