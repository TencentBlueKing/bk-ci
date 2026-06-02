package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.RepoAuthCopyResourceProp
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制任务保存服务
 */
@Service
class PipelineCopyTaskSaveService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyResourceGetService: PipelineCopyResourceGetService
) {

    fun saveConfigDraft(
        userId: String,
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskConfigRequest
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        try {
            lock.lock()
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
            if (task.status != PipelineBatchTaskStatus.DRAFT) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_SAVE_CONFIG,
                    params = arrayOf(taskId, task.status.name)
                )
            }
            val oldParam = parseParam(task)
            val param = PipelineBatchCopyTaskParam(
                targetProjectId = request.targetProjectId,
                pipelineCopyStrategy = request.pipelineCopyStrategy
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineBatchTaskDao.update(
                    dslContext = transactionContext,
                    update = PipelineBatchTaskUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        taskName = request.taskName,
                        taskParam = JsonUtil.toJson(param, formatted = false),
                    )
                )
                if (oldParam != null && oldParam.targetProjectId != request.targetProjectId) {
                    pipelineBatchTaskDetailDao.updateChange(
                        dslContext = transactionContext,
                        projectId = projectId,
                        taskId = taskId,
                        change = true
                    )
                }
            }
        } finally {
            lock.unlock()
        }
    }

    fun saveResourceDraft(
        userId: String,
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val task = tryStartAnalyze(projectId = projectId, taskId = taskId) ?: return
        parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        saveResources(
            projectId = projectId,
            taskId = taskId,
            resources = resources
        )
        finishSave(projectId = projectId, taskId = taskId)
    }

    private fun saveResources(
        projectId: String,
        taskId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val resourceUpdates = resources.map { resource ->
            val copyStrategy = resource.copyStrategy ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_CAN_NOT_EMPTY,
                params = arrayOf(resource.resourceType.name, resource.resourceName)
            )
            if (!copyStrategy.support(resource.resourceType)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
                    params = arrayOf(
                        resource.resourceName,
                        copyStrategy.name,
                        resource.resourceType.name
                    )
                )
            }
            when (copyStrategy) {
                PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET -> {
                    val targetResourceId = resource.targetResourceId
                    if (targetResourceId.isNullOrBlank()) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                            params = arrayOf(resource.resourceName, resource.resourceType.name, copyStrategy.name)
                        )
                    }
                    PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        targetResourceType = resource.resourceType,
                        targetResourceId = targetResourceId,
                        status = PipelineCopyTaskResourceStatus.PROCESSED,
                        copyStrategy = copyStrategy,
                        copyAction = copyStrategy.copyAction
                    )
                }

                PipelineCopyStrategy.REPOSITORY_CREATE_NEW -> {
                    pipelineCopyResourceGetService.validateRepositoryProperties(resource)
                    PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        targetResourceType = resource.resourceType,
                        targetResourceProperties = resource.targetResourceProp as RepoAuthCopyResourceProp,
                        status = PipelineCopyTaskResourceStatus.PROCESSED,
                        copyStrategy = copyStrategy,
                        copyAction = copyStrategy.copyAction
                    )
                }

                else -> {
                    PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        targetResourceType = resource.resourceType,
                        status = PipelineCopyTaskResourceStatus.PROCESSED,
                        copyStrategy = copyStrategy,
                        copyAction = copyStrategy.copyAction,
                        highRisk = copyStrategy.highRisk
                    )
                }
            }
        }
        pipelineCopyTaskResourceDao.batchUpdate(
            dslContext = dslContext,
            updates = resourceUpdates
        )
    }

    private fun finishSave(
        projectId: String,
        taskId: String
    ) {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        try {
            lock.lock()
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.DRAFT
            )
        } finally {
            lock.unlock()
        }
    }

    private fun tryStartAnalyze(
        projectId: String,
        taskId: String
    ): PipelineBatchTask? {
        val lock = PipelineCopyTaskLock(
            redisOperation = redisOperation,
            projectId = projectId,
            taskId = taskId
        )
        // 分析阶段,耗时比较长,锁只需要加在更新状态阶段
        try {
            lock.lock()
            val task = pipelineBatchTaskDao.get(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId
            ) ?: run {
                logger.warn("pipeline batch task has no change, no need to analyze|$projectId|$taskId")
                return null
            }
            if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
                logger.warn("pipeline batch task type not match|$projectId|$taskId")
                return null
            }
            if (task.status != PipelineBatchTaskStatus.DRAFT) {
                logger.warn("pipeline batch task status not match|$projectId|$taskId|${task.status}")
                return null
            }
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.SAVING
            )
            return task
        } finally {
            lock.unlock()
        }
    }

    private fun parseParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam? {
        return task.taskParam?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskSaveService::class.java)
    }
}
