package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制任务重试服务
 */
@Service
class PipelineCopyTaskRetryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao
) {

    fun retry(
        projectId: String,
        taskId: String
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        val task: PipelineBatchTask
        try {
            lock.lock()
            task = getRetryableTask(projectId = projectId, taskId = taskId)
            val failedDetails = pipelineBatchTaskDetailDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskDetailStatus.FAILED
            )
            val failedResources = pipelineCopyTaskResourceDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.FAILED
            )
            if (failedDetails.isEmpty() && failedResources.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOTHING_TO_RETRY,
                    params = arrayOf(taskId)
                )
            }
            resetAndQueueExecute(
                projectId = projectId,
                taskId = taskId,
                failedDetails = failedDetails.map { it.pipelineId }.toSet(),
                failedResources = failedResources
            )
        } finally {
            lock.unlock()
        }
        dispatchExecuteEvent(task = task)
    }

    fun retryPipeline(
        projectId: String,
        taskId: String,
        pipelineId: String
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        val task: PipelineBatchTask
        try {
            lock.lock()
            task = getRetryableTask(projectId = projectId, taskId = taskId)
            val detail = pipelineBatchTaskDetailDao.get(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
            if (detail.status != PipelineBatchTaskDetailStatus.FAILED) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_DETAIL_CAN_NOT_RETRY,
                    params = arrayOf(pipelineId, detail.status.name)
                )
            }
            val failedResources = listFailedPipelineResources(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId
            )
            resetAndQueueExecute(
                projectId = projectId,
                taskId = taskId,
                failedDetails = setOf(pipelineId),
                failedResources = failedResources
            )
        } finally {
            lock.unlock()
        }
        dispatchExecuteEvent(task = task)
    }

    private fun getRetryableTask(
        projectId: String,
        taskId: String
    ): PipelineBatchTask {
        val task = pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_NOT_EXISTS,
            params = arrayOf(taskId)
        )
        if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_TYPE_NOT_MATCH,
                params = arrayOf(taskId, task.taskType.name, PipelineBatchTaskType.PIPELINE_COPY.name)
            )
        }
        if (task.status !in RETRYABLE_TASK_STATUSES) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_RETRY,
                params = arrayOf(taskId, task.status.name)
            )
        }
        return task
    }

    private fun listFailedPipelineResources(
        projectId: String,
        taskId: String,
        pipelineId: String
    ): List<PipelineCopyTaskResource> {
        val relations = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = setOf(pipelineId)
        )
        if (relations.isEmpty()) {
            return emptyList()
        }
        val resourceMap = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineCopyTaskResourceStatus.FAILED
        ).associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }
        return relations.mapNotNull { relation ->
            resourceMap[
                PipelineCopyTaskUtils.resourceKey(
                    resourceType = relation.resourceType,
                    resourceId = relation.resourceId
                )
            ]
        }
    }

    private fun resetAndQueueExecute(
        projectId: String,
        taskId: String,
        failedDetails: Set<String>,
        failedResources: List<PipelineCopyTaskResource>
    ) {
        val resourceUpdates = failedResources.map { resource ->
            PipelineCopyTaskResourceUpdate(
                projectId = projectId,
                taskId = taskId,
                resourceType = resource.resourceType,
                resourceId = resource.resourceId,
                status = PipelineCopyTaskResourceStatus.PROCESSED,
                clearErrorMessage = true
            )
        }
        val detailUpdates = failedDetails.map { pipelineId ->
            PipelineBatchTaskDetailUpdate(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId,
                status = PipelineBatchTaskDetailStatus.WAIT_COPY,
                clearErrorMessage = true,
                clearErrorType = true
            )
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            if (resourceUpdates.isNotEmpty()) {
                pipelineCopyTaskResourceDao.batchUpdate(
                    dslContext = transactionContext,
                    updates = resourceUpdates
                )
            }
            if (detailUpdates.isNotEmpty()) {
                pipelineBatchTaskDetailDao.batchUpdate(
                    dslContext = transactionContext,
                    updates = detailUpdates
                )
            }
            pipelineBatchTaskDao.update(
                dslContext = transactionContext,
                update = PipelineBatchTaskUpdate(
                    projectId = projectId,
                    taskId = taskId,
                    status = PipelineBatchTaskStatus.EXECUTE_QUEUED,
                    clearErrorMessage = true
                )
            )
        }
    }

    private fun dispatchExecuteEvent(task: PipelineBatchTask) {
        sampleEventDispatcher.dispatch(
            PipelineBatchTaskExecuteEvent(
                taskId = task.taskId,
                taskType = task.taskType,
                projectId = task.projectId
            )
        )
    }

    companion object {
        private val RETRYABLE_TASK_STATUSES = setOf(
            PipelineBatchTaskStatus.FAILED,
            PipelineBatchTaskStatus.PARTIAL_FAILED
        )
    }
}
