package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
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
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
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
    private val pipelineIdGenerator: PipelineIdGenerator
) {

    fun saveConfigDraft(
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
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest
    ) {
        val task = tryStartSave(projectId = projectId, taskId = taskId)
        try {
            val param = parseParam(task) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
                params = arrayOf(task.taskId)
            )

            val newParam = param.copy(
                pipelineLabelCopyStrategy = request.pipelineLabelCopyStrategy,
                pipelineGroupCopyStrategy = request.pipelineGroupCopyStrategy
            )
            val resourceUpdates = buildCopyTaskResourceUpdates(
                projectId = projectId,
                taskId = taskId,
                request = request
            )
            dslContext.transaction { configuration ->
                val transactionContext = DSL.using(configuration)
                pipelineBatchTaskDao.update(
                    dslContext = transactionContext,
                    update = PipelineBatchTaskUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        status = PipelineBatchTaskStatus.DRAFT,
                        taskParam = JsonUtil.toJson(newParam, formatted = false),
                    )
                )
                pipelineCopyTaskResourceDao.batchUpdate(
                    dslContext = transactionContext,
                    updates = resourceUpdates
                )
            }
        } catch (exception: Exception) {
            logger.error("Failed to save resource draft|$projectId|$taskId", exception)
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.DRAFT
            )
            throw exception
        }
    }

    private fun buildCopyTaskResourceUpdates(
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest
    ): List<PipelineCopyTaskResourceUpdate> {
        val storedResources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = request.resources.map { it.resourceId }.toSet()
        )
        val storedResourceMap = storedResources.associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }
        val resourceUpdates = mutableListOf<PipelineCopyTaskResourceUpdate>()
        resourceUpdates.addAll(
            buildCopyTaskResourceUpdates(
                projectId = projectId,
                taskId = taskId,
                request = request,
                storedResourceMap = storedResourceMap
            )
        )
        request.pipelineGroupCopyStrategy?.let {
            resourceUpdates.addAll(
                buildResourceUpdatesByResourceType(
                    projectId = projectId,
                    taskId = taskId,
                    storedResources = storedResources,
                    copyStrategy = it,
                    resourceType = PipelineDependentResourceType.PIPELINE_GROUP
                )
            )
        }
        request.pipelineLabelCopyStrategy?.let {
            resourceUpdates.addAll(
                buildResourceUpdatesByResourceType(
                    projectId = projectId,
                    taskId = taskId,
                    storedResources = storedResources,
                    copyStrategy = it,
                    resourceType = PipelineDependentResourceType.PIPELINE_LABEL
                )
            )
        }
        return resourceUpdates
    }

    @Suppress("LongMethod")
    private fun buildCopyTaskResourceUpdates(
        projectId: String,
        taskId: String,
        request: PipelineCopyTaskSaveResourceRequest,
        storedResourceMap: Map<String, PipelineCopyTaskResource>
    ): List<PipelineCopyTaskResourceUpdate> {
        // copyStrategy为空,说明用户还没处理这个资源
        // 流水线组和流水线标签,不需要处理前端传入的资源
        val resourceUpdates = request.resources.filter {
            it.copyStrategy != null &&
                it.resourceType != PipelineDependentResourceType.PIPELINE_GROUP &&
                it.resourceType != PipelineDependentResourceType.PIPELINE_LABEL
        }.map { resource ->
            val resourceKey = PipelineCopyTaskUtils.resourceKey(
                resourceType = resource.resourceType,
                resourceId = resource.resourceId
            )
            val storeResource = storedResourceMap[resourceKey] ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_NOT_EXISTS,
                params = arrayOf(taskId, "${resource.resourceType.name}:${resource.resourceName}")
            )
            val copyStrategy = resource.copyStrategy!!
            if (!copyStrategy.support(resource.resourceType)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
                    params = arrayOf(
                        resource.resourceType.name,
                        resource.resourceName,
                        copyStrategy.name
                    )
                )
            }
            when (copyStrategy) {
                PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET -> {
                    val targetResourceId = resource.targetResourceId
                    if (targetResourceId.isNullOrBlank()) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                            params = arrayOf(resource.resourceType.name, resource.resourceName, copyStrategy.name)
                        )
                    }
                    PipelineCopyTaskResourceUpdate(
                        projectId = projectId,
                        taskId = taskId,
                        resourceType = resource.resourceType,
                        resourceId = resource.resourceId,
                        targetResourceType = resource.resourceType,
                        targetResourceId = targetResourceId,
                        targetResourceName = targetResourceId,
                        status = PipelineCopyTaskResourceStatus.PROCESSED,
                        copyStrategy = copyStrategy,
                        copyAction = copyStrategy.copyAction,
                        highRisk = copyStrategy.highRisk
                    )
                }

                PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {
                    val targetResourceId = if (storeResource.targetIdExists) {
                        pipelineIdGenerator.getNextId()
                    } else {
                        resource.resourceId
                    }
                    val targetResourceName = if (storeResource.targetNameExists) {
                        if (resource.targetResourceName.isNullOrBlank()) {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                                params = arrayOf(resource.resourceType.name, resource.resourceName, copyStrategy.name)
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

                PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL,
                PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME,
                PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME,
                PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME,
                PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME,
                PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME,
                PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME -> {
                    if (!storeResource.targetIdExists) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_REUSE_RESOURCE_NOT_EXISTS,
                            params = arrayOf(resource.resourceType.name, resource.resourceName)
                        )
                    }
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
        return resourceUpdates
    }

    private fun buildResourceUpdatesByResourceType(
        projectId: String,
        taskId: String,
        storedResources: List<PipelineCopyTaskResource>,
        copyStrategy: PipelineCopyStrategy,
        resourceType: PipelineDependentResourceType
    ): List<PipelineCopyTaskResourceUpdate> {
        if (!copyStrategy.support(resourceType)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_TYPE_STRATEGY_NOT_SUPPORT,
                params = arrayOf(
                    resourceType.name,
                    copyStrategy.name
                )
            )
        }
        return storedResources.filter {
            it.resourceType == resourceType
        }.map { resource ->
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

    private fun tryStartSave(
        projectId: String,
        taskId: String
    ): PipelineBatchTask {
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
