package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSaveResourceRequest
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
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val authProjectApi: AuthProjectApi,
    private val pipelineCopyTaskStateService: PipelineCopyTaskStateService
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
        request: PipelineCopyTaskSaveResourceRequest
    ) {
        val task = tryStartSave(projectId = projectId, taskId = taskId)
        val param = parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        checkProjectManager(userId = userId, projectId = projectId)
        checkProjectManager(userId = userId, projectId = param.targetProjectId)
        saveResources(
            projectId = projectId,
            taskId = taskId,
            request = request
        )
        finishSave(projectId = projectId, taskId = taskId)
    }

    private fun saveResources(
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest
    ) {
        val storedResources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = request.resources.map { it.resourceId }.toSet()
        ).associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }
        val resourceUpdates = request.resources.map { resource ->
            val resourceKey = PipelineCopyTaskUtils.resourceKey(
                resourceType = resource.resourceType,
                resourceId = resource.resourceId
            )
            val storeResource = storedResources[resourceKey] ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_NOT_EXISTS,
                params = arrayOf(taskId, "${resource.resourceType.name}:${resource.resourceName}")
            )
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

                PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {
                    var targetResourceId: String? = null
                    if (storeResource.targetIdExists) {
                        targetResourceId = pipelineIdGenerator.getNextId()
                    }
                    val targetResourceName = if (storeResource.targetNameExists) {
                        if (resource.targetResourceName.isNullOrBlank()) {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                                params = arrayOf(resource.resourceName, resource.resourceType.name, copyStrategy.name)
                            )
                        } else {
                            resource.targetResourceName
                        }

                    } else {
                        resource.resourceName
                    }
                    PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        targetResourceType = resource.resourceType,
                        targetResourceId = targetResourceId,
                        targetResourceName = targetResourceName,
                        status = PipelineCopyTaskResourceStatus.PROCESSED,
                        copyStrategy = copyStrategy,
                        copyAction = copyStrategy.copyAction,
                        highRisk = copyStrategy.highRisk
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
        pipelineCopyTaskStateService.updateTaskStatusWithLock(
            projectId = projectId,
            taskId = taskId,
            status = PipelineBatchTaskStatus.DRAFT
        )
    }

    private fun tryStartSave(
        projectId: String,
        taskId: String
    ): PipelineBatchTask {
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

    private fun checkProjectManager(
        userId: String,
        projectId: String
    ) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskSaveService::class.java)
    }
}
