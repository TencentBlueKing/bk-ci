package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
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
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.CredentialBasicInfo
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
    private val client: Client
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
        val param = parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        saveResources(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = param.targetProjectId,
            resources = resources
        )
        finishSave(projectId = projectId, taskId = taskId)
    }

    private fun saveResources(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        saveCredentials(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        saveRepositories(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        saveNodes(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        saveEnvs(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        savePipelineGroups(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        savePipelineLabels(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        savePipelineTemplates(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        savePipelines(
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
    }

    private fun saveCredentials(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.CREDENTIAL
        }.forEach {
            saveCredential(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun saveCredential(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME -> {
                updateCopyResource(
                    projectId = projectId,
                    taskId = taskId,
                    resource = resource,
                    copyStrategy = copyStrategy
                )
            }

            PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET -> {
                val targetCredentialId = resource.targetResourceId
                if (targetCredentialId.isNullOrBlank()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                        params = arrayOf(resource.resourceName, resource.resourceType.name, copyStrategy.name)
                    )
                }
                updateCopyResource(
                    projectId = projectId,
                    taskId = taskId,
                    resource = resource,
                    copyStrategy = copyStrategy,
                    targetResourceId = targetCredentialId
                )
            }

            PipelineCopyStrategy.CREDENTIAL_CREATE_NEW -> {
                updateCopyResource(
                    projectId = projectId,
                    taskId = taskId,
                    resource = resource,
                    copyStrategy = copyStrategy,
                    targetResourceId = resource.resourceId,
                    highRisk = true
                )
            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun validateCredential(
        userId: String,
        targetProjectId: String,
        credentialId: String,
        checkName: String,
        expectExists: Boolean
    ): CredentialBasicInfo? {
        return validateTargetResourceNameExists(
            targetProjectId = targetProjectId,
            targetResourceName = checkName,
            expectExists = expectExists
        ) {
            client.get(ServiceCredentialResource::class).getBasicInfo(
                userId = userId,
                projectId = targetProjectId,
                credentialId = credentialId
            ).data
        }
    }

    /**
     * 验证目标项目资源名称是否符合预期。
     *
     * expectExists = true: 目标资源必须存在,用于复用或替换场景。
     * expectExists = false: 目标资源必须不存在,用于创建新资源前的冲突检查。
     */
    private fun <T> validateTargetResourceNameExists(
        targetProjectId: String,
        targetResourceName: String,
        expectExists: Boolean,
        getResource: () -> T?
    ): T? {
        val targetResource = try {
            getResource()
        } catch (ignored: RemoteServiceException) {
            null
        }
        if (expectExists && targetResource == null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_NOT_EXISTS,
                params = arrayOf(targetProjectId, targetResourceName)
            )
        }
        if (!expectExists && targetResource != null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EXISTS,
                params = arrayOf(targetProjectId, targetResourceName)
            )
        }
        return targetResource
    }

    private fun updateCopyResource(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource,
        copyStrategy: PipelineCopyStrategy,
        targetResourceId: String? = null,
        targetResourceName: String? = null,
        highRisk: Boolean = false
    ) {
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
                projectId = projectId,
                taskId = taskId,
                resourceType = resource.resourceType,
                resourceId = resource.resourceId,
                targetResourceType = targetResourceId?.let { resource.resourceType },
                targetResourceId = targetResourceId,
                targetResourceName = targetResourceName,
                status = PipelineCopyTaskResourceStatus.PROCESSED,
                copyStrategy = copyStrategy,
                copyAction = copyStrategy.copyAction,
                highRisk = highRisk
            )
        )
    }

    private fun unsupportedCopyStrategy(resource: PipelineCopyTaskResource): Nothing {
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
            params = arrayOf(
                resource.resourceName,
                resource.copyStrategy?.name ?: "",
                resource.resourceType.name
            )
        )
    }

    private fun saveRepositories(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.REPOSITORY
        }.forEach {
            saveRepository(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun saveRepository(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL -> {
                updateCopyResource(
                    projectId = projectId,
                    taskId = taskId,
                    resource = resource,
                    copyStrategy = copyStrategy
                )
            }

            PipelineCopyStrategy.REPOSITORY_CREATE_NEW -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun saveEnvs(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_ENV ||
                it.resourceType == PipelineDependentResourceType.DEPLOY_ENV
        }.forEach {
            saveEnv(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun saveEnv(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE -> {

            }

            PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE -> {

            }

            PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun saveNodes(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_NODE ||
                it.resourceType == PipelineDependentResourceType.DEPLOY_NODE
        }.forEach {
            saveNode(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun saveNode(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT -> {

            }

            PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun savePipelineGroups(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP
        }.forEach {
            savePipelineGroup(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun savePipelineGroup(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE -> {

            }

            PipelineCopyStrategy.PIPELINE_GROUP_IGNORE -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun savePipelineLabels(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_LABEL
        }.forEach {
            savePipelineLabel(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun savePipelineLabel(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE -> {

            }

            PipelineCopyStrategy.LABEL_IGNORE -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun savePipelineTemplates(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_TEMPLATE
        }.forEach {
            savePipelineTemplate(
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun savePipelineTemplate(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
    }

    private fun savePipelines(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE
        }.forEach {
            savePipeline(projectId = projectId, taskId = taskId, targetProjectId = targetProjectId, resource = it)
        }
    }

    private fun savePipeline(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (val copyStrategy = resource.copyStrategy) {
            PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID -> {

            }

            PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID -> {

            }

            PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {

            }

            PipelineCopyStrategy.PIPELINE_SKIP -> {

            }

            else -> {
                unsupportedCopyStrategy(resource)
            }
        }
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
