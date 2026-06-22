package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailErrorType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStep
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskErrorMessage
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineLabelCopyResourceProp
import com.tencent.devops.process.pojo.pipeline.task.PipelineTemplateCopyResourceProp
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制任务执行服务
 */
@Service
class PipelineCopyTaskExecuteService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val pipelineCopyResourceGetService: PipelineCopyResourceGetService,
    private val pipelineCopyResourceCreateService: PipelineCopyResourceCreateService,
    private val pipelineCopyTemplateCreateService: PipelineCopyTemplateCreateService,
    private val pipelineCopyTaskStateService: PipelineCopyTaskStateService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator
) {
    fun confirmExecute(
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
            if (task.status !in setOf(
                    PipelineBatchTaskStatus.DRAFT,
                    PipelineBatchTaskStatus.EXECUTE_FAILED
                )
            ) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_BATCH_TASK_STATUS_CAN_NOT_EXECUTE,
                    params = arrayOf(taskId, task.status.name)
                )
            }
            if (PipelineCopyTaskUtils.parseParam(task) == null) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
                    params = arrayOf(taskId)
                )
            }
            val unprocessedCount = pipelineCopyTaskResourceDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.UNPROCESSED
            )
            if (unprocessedCount > 0) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_NOT_ALL_PROCESSED,
                    params = arrayOf(taskId, unprocessedCount.toString())
                )
            }
            pipelineBatchTaskDao.update(
                dslContext = dslContext,
                update = PipelineBatchTaskUpdate(
                    projectId = projectId,
                    taskId = taskId,
                    step = PipelineBatchTaskStep.EXECUTE,
                    status = PipelineBatchTaskStatus.EXECUTE_QUEUED,
                    clearErrorMessage = true
                )
            )
        } finally {
            lock.unlock()
        }
    }

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        with(event) {
            logger.info("start to execute pipeline copy task|$projectId|$taskId")
            val task = tryStartExecute(projectId = projectId, taskId = taskId) ?: return
            try {
                val resources = pipelineCopyTaskResourceDao.list(
                    dslContext = dslContext,
                    projectId = projectId,
                    taskId = taskId,
                    status = PipelineCopyTaskResourceStatus.PROCESSED
                )
                val param = PipelineCopyTaskUtils.parseParam(task) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
                    params = arrayOf(task.taskId)
                )
                executeDependentResources(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executePipelines(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId
                )
                finishExecute(projectId = projectId, taskId = taskId)
            } catch (ignored: Exception) {
                logger.error("Failed to execute pipeline copy task|$projectId|$taskId", ignored)
                pipelineCopyTaskStateService.updateTaskStatusWithLock(
                    projectId = projectId,
                    taskId = taskId,
                    status = PipelineBatchTaskStatus.EXECUTE_FAILED,
                    errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
                )
            }
        }
    }

    private fun executeDependentResources(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        // 凭证要在代码库之前创建
        executeCredentials(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        executeRepositories(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        // 节点要在环境之前创建
        executeNodes(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        executeEnvs(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        executePipelineGroups(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        executePipelineLabels(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
        executePipelineTemplates(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources
        )
    }

    private fun tryStartExecute(
        projectId: String,
        taskId: String
    ): PipelineBatchTask? {
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
            ) ?: run {
                logger.warn("pipeline batch task not found|$projectId|$taskId")
                return null
            }
            if (task.taskType != PipelineBatchTaskType.PIPELINE_COPY) {
                logger.warn("pipeline batch task type not match|$projectId|$taskId")
                return null
            }
            if (task.status != PipelineBatchTaskStatus.EXECUTE_QUEUED) {
                logger.warn("pipeline batch task status not match|$projectId|$taskId")
                return null
            }
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.EXECUTING
            )
            return task
        } finally {
            lock.unlock()
        }
    }

    private fun executeCredentials(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.CREDENTIAL
        }.map {
            executeCredential(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executeCredential(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource = resource)) {
                PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME -> {
                    val targetResource = pipelineCopyResourceGetService.getCredentialBasicInfo(
                        userId = userId,
                        projectId = targetProjectId,
                        credentialId = resource.resourceId,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET -> {
                    val targetCredentialId = resource.targetResourceId
                    if (targetCredentialId.isNullOrBlank()) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                            params = arrayOf(resource.resourceType.name, resource.resourceName, copyStrategy.name)
                        )
                    }
                    val targetResource = pipelineCopyResourceGetService.getCredentialBasicInfo(
                        userId = userId,
                        projectId = targetProjectId,
                        credentialId = targetCredentialId,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.CREDENTIAL_CREATE_NEW -> {
                    val targetResource = pipelineCopyResourceCreateService.createCredential(
                        userId = userId,
                        sourceProjectId = projectId,
                        credentialId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceId
                }

                else -> {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
                        params = arrayOf(
                            resource.resourceType.name,
                            resource.resourceName,
                            copyStrategy.name
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute credential failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executeRepositories(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        // 不能直接使用resources,需要再查询获取最新的资源
        val latestResources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        val resourceMap = latestResources.associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }.toMutableMap()
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.REPOSITORY
        }.map {
            executeRepository(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it,
                resourceMap = resourceMap
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executeRepository(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            val copyStrategy = validateCopyStrategy(resource = resource)
            when (resource.copyStrategy!!) {
                PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL -> {
                    val targetResource = pipelineCopyResourceGetService.getRepositoryByName(
                        userId = userId,
                        projectId = targetProjectId,
                        repositoryName = resource.resourceName,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.REPOSITORY_CREATE_NEW -> {
                    val targetResource = pipelineCopyResourceCreateService.createRepository(
                        userId = userId,
                        sourceProjectId = projectId,
                        repoName = resource.resourceName,
                        targetProjectId = targetProjectId,
                        resourceMap = resourceMap
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                else -> {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
                        params = arrayOf(
                            resource.resourceType.name,
                            resource.resourceName,
                            copyStrategy.name
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute repository failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executeEnvs(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_ENV ||
                    it.resourceType == PipelineDependentResourceType.DEPLOY_ENV
        }.map {
            executeEnv(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executeEnv(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME,
                PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME -> {
                    val targetResource = pipelineCopyResourceGetService.getEnvByName(
                        userId = userId,
                        projectId = targetProjectId,
                        envName = resource.resourceName,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE,
                PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE -> {
                    val targetResource = pipelineCopyResourceCreateService.createEnv(
                        userId = userId,
                        sourceProjectId = projectId,
                        sourceEnvHashId = resource.resourceId,
                        targetProjectId = targetProjectId,
                        nodeHashIds = emptyList()
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE,
                PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE -> {
                    val targetResource = pipelineCopyResourceCreateService.createEnvAndMoveNodes(
                        userId = userId,
                        sourceProjectId = projectId,
                        sourceEnvHashId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_ENV_CREATE_AND_REUSE_SAME_NAME_NODE,
                PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_REUSE_SAME_NAME_NODE -> {
                    val targetResource = pipelineCopyResourceCreateService.createEnvAndRelateSameNameNodes(
                        userId = userId,
                        sourceProjectId = projectId,
                        sourceEnvHashId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute env failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executeNodes(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_NODE ||
                    it.resourceType == PipelineDependentResourceType.DEPLOY_NODE
        }.map {
            executeNode(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executeNode(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME -> {
                    val targetResource = pipelineCopyResourceGetService.getBuildNodeByName(
                        userId = userId,
                        projectId = targetProjectId,
                        nodeName = resource.resourceName,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME -> {
                    val targetResource = pipelineCopyResourceGetService.getDeployNodeByName(
                        userId = userId,
                        projectId = targetProjectId,
                        nodeName = resource.resourceName,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT -> {
                    // 构建节点,存储的是构建机agentId
                    val targetResource = pipelineCopyResourceCreateService.moveBuildNodeToTargetProject(
                        userId = userId,
                        sourceProjectId = projectId,
                        agentHashId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT -> {
                    val targetResource = pipelineCopyResourceCreateService.moveDeployNodeToTargetProject(
                        userId = userId,
                        sourceProjectId = projectId,
                        nodeHashId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_NODE_SKIP,
                PipelineCopyStrategy.DEPLOY_NODE_SKIP -> {
                    status = PipelineCopyTaskResourceStatus.SKIP
                }

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute node failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executePipelineGroups(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP
        }.map {
            executePipelineGroup(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executePipelineGroup(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE -> {
                    val pipelineGroup = pipelineCopyResourceGetService.getPipelineViewByName(
                        projectId = targetProjectId,
                        viewName = resource.resourceName,
                        expectExists = null
                    ) ?: pipelineCopyResourceCreateService.createPipelineGroup(
                        userId = userId,
                        sourceProjectId = projectId,
                        viewName = resource.resourceName,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = pipelineGroup.resourceId
                    targetResourceName = pipelineGroup.resourceName
                }

                PipelineCopyStrategy.PIPELINE_GROUP_IGNORE -> Unit

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute pipeline group failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executePipelineLabels(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val updates = resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_LABEL
        }.map {
            executePipelineLabel(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executePipelineLabel(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE -> {
                    val labelGroupProp = resource.resourceProperties as? PipelineLabelCopyResourceProp
                        ?: throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
                            params = arrayOf(projectId, resource.resourceId)
                        )
                    val pipelineLabel = pipelineCopyResourceGetService.getPipelineLabelByGroupAndName(
                        projectId = targetProjectId,
                        groupName = labelGroupProp.groupName,
                        labelName = resource.resourceName,
                        expectExists = null
                    ) ?: pipelineCopyResourceCreateService.createPipelineLabel(
                        userId = userId,
                        sourceProjectId = projectId,
                        sourceLabelId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = pipelineLabel.resourceId
                    targetResourceName = pipelineLabel.resourceName
                }

                PipelineCopyStrategy.LABEL_IGNORE -> Unit

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute pipeline label failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun executePipelineTemplates(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val templateResources = resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_TEMPLATE
        }
        if (templateResources.isEmpty()) {
            return
        }
        val latestResources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        val resourceMap = latestResources.associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }
        val updates = templateResources.map {
            executePipelineTemplate(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it,
                resourceMap = resourceMap
            )
        }
        pipelineCopyTaskResourceDao.batchUpdate(dslContext = dslContext, updates = updates)
    }

    private fun executePipelineTemplate(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var targetResourceProp: PipelineTemplateCopyResourceProp? = null
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME -> {
                    val targetTemplate = pipelineCopyResourceGetService.getTemplateByName(
                        projectId = targetProjectId,
                        templateName = resource.resourceName,
                        expectExists = true
                    )!!
                    targetResourceId = targetTemplate.resourceId
                    targetResourceName = targetTemplate.resourceName
                    targetResourceProp = copyTemplateVersions(
                        userId = userId,
                        projectId = projectId,
                        taskId = taskId,
                        resource = resource,
                        targetProjectId = targetProjectId,
                        targetTemplateId = targetTemplate.resourceId,
                        resourceMap = resourceMap
                    )
                }

                PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW -> {
                    pipelineCopyResourceGetService.getTemplateByName(
                        projectId = targetProjectId,
                        templateName = resource.resourceName,
                        expectExists = false
                    )
                    val newTemplateId = pipelineTemplateGenerator.generateTemplateId()
                    targetResourceId = newTemplateId
                    targetResourceName = resource.resourceName
                    targetResourceProp = copyTemplateVersions(
                        userId = userId,
                        projectId = projectId,
                        taskId = taskId,
                        resource = resource,
                        targetProjectId = targetProjectId,
                        targetTemplateId = newTemplateId,
                        resourceMap = resourceMap
                    )
                }

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute pipeline template failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            targetResourceProperties = targetResourceProp,
            errorMessage = errorMessage
        )
    }

    private fun copyTemplateVersions(
        userId: String,
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource,
        targetProjectId: String,
        targetTemplateId: String,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): PipelineTemplateCopyResourceProp? {
        val sourceVersions = collectSourceTemplateVersions(
            projectId = projectId,
            taskId = taskId,
            resource = resource
        )
        if (sourceVersions.isEmpty()) {
            return null
        }
        val versionMappings = sourceVersions.map { sourceVersion ->
            pipelineCopyTemplateCreateService.createTemplateVersion(
                userId = userId,
                sourceProjectId = projectId,
                sourceTemplateId = resource.resourceId,
                sourceTemplateVersion = sourceVersion,
                targetProjectId = targetProjectId,
                targetTemplateId = targetTemplateId,
                resourceMap = resourceMap
            )
        }
        return PipelineTemplateCopyResourceProp(versionMappings = versionMappings)
    }

    private fun collectSourceTemplateVersions(
        projectId: String,
        taskId: String,
        resource: PipelineCopyTaskResource
    ): Set<Long> {
        val templateInfo = pipelineTemplateInfoService.getOrNull(
            projectId = projectId,
            templateId = resource.resourceId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(projectId, resource.resourceId)
        )
        val pipelineIds = pipelineCopyTaskResourceRelDao.listResourcePipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceType = PipelineDependentResourceType.PIPELINE_TEMPLATE,
            resourceId = resource.resourceId
        )
        // 如果没有关联的流水线,那么不需要创建版本
        if (pipelineIds.isEmpty()) {
            return emptySet()
        }
        // 仅创建关联的模版版本和模版最新版本
        val templateVersions = pipelineTemplateRelatedService.listByPipelineIds(
            projectId = projectId,
            pipelineIds = pipelineIds
        ).filter { it.templateId == resource.resourceId }
            .map { it.version }.toMutableSet()
        if (templateInfo.releasedVersion != 0L && !templateVersions.contains(templateInfo.releasedVersion)) {
            templateVersions.add(templateInfo.releasedVersion)
        }
        return templateVersions
    }

    /**
     * 分析流水线执行链路，返回叶子优先的执行顺序列表。
     * - 把本任务内 PIPELINE→PIPELINE 关系组织成调用森林
     * - DFS 后序遍历：子流水线先于父流水线
     * - visited 集合负责防重 + 防循环
     */
    private fun analyzePipelineExecutionOrder(
        projectId: String,
        taskId: String,
        details: List<PipelineBatchTaskDetail>
    ): List<PipelineBatchTaskDetail> {
        if (details.isEmpty()) {
            return emptyList()
        }
        val detailMap = details.associateBy { it.pipelineId }
        val parentToSubs = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = detailMap.keys,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).filter { rel ->
            rel.pipelineId != rel.resourceId && detailMap.containsKey(rel.resourceId)
        }.groupBy({ it.pipelineId }, { it.resourceId })
        val visited = mutableSetOf<String>()
        val ordered = mutableListOf<PipelineBatchTaskDetail>()

        fun dfs(pipelineId: String) {
            if (!visited.add(pipelineId)) {
                return
            }
            parentToSubs[pipelineId]?.forEach { subPipelineId ->
                dfs(subPipelineId)
            }
            detailMap[pipelineId]?.let { ordered.add(it) }
        }

        details.forEach { detail ->
            dfs(detail.pipelineId)
        }
        logger.info(
            "pipeline copy execution order|$projectId|$taskId|" +
                ordered.joinToString(",") { it.pipelineId }
        )
        return ordered
    }

    private fun executePipelines(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String
    ) {
        val details = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY,
            change = false
        )
        if (details.isEmpty()) {
            return
        }
        val latestResources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        )
        val resourceMap = latestResources.associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }.toMutableMap()
        val pipelineResourceMap = latestResources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE
        }.associateBy { it.resourceId }
        val executionOrder = analyzePipelineExecutionOrder(
            projectId = projectId,
            taskId = taskId,
            details = details
        )
        executionOrder.forEach { detail ->
            executePipeline(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                detail = detail,
                resource = pipelineResourceMap[detail.pipelineId],
                resourceMap = resourceMap
            )
        }
    }

    private fun executePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        detail: PipelineBatchTaskDetail,
        resource: PipelineCopyTaskResource?,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ) {
        val pipelineResource = validatePipelineExecutionPreconditions(
            projectId = projectId,
            taskId = taskId,
            detail = detail,
            resource = resource,
            resourceMap = resourceMap
        ) ?: return

        updatePipelineDetailStatus(
            projectId = projectId,
            taskId = taskId,
            pipelineId = detail.pipelineId,
            status = PipelineBatchTaskDetailStatus.EXECUTING
        )

        val resourceUpdate = copyPipelineResource(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resource = pipelineResource,
            resourceMap = resourceMap
        )

        persistPipelineExecution(
            projectId = projectId,
            taskId = taskId,
            pipelineId = detail.pipelineId,
            resource = pipelineResource,
            resourceUpdate = resourceUpdate,
            resourceMap = resourceMap
        )
    }

    private fun validatePipelineExecutionPreconditions(
        projectId: String,
        taskId: String,
        detail: PipelineBatchTaskDetail,
        resource: PipelineCopyTaskResource?,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): PipelineCopyTaskResource? {
        if (resource == null) {
            val exception = ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_NOT_EXISTS,
                params = arrayOf(projectId, detail.pipelineName)
            )
            updatePipelineDetailStatus(
                projectId = projectId,
                taskId = taskId,
                pipelineId = detail.pipelineId,
                status = PipelineBatchTaskDetailStatus.FAILED,
                errorType = PipelineBatchTaskDetailErrorType.DEPENDENCY_CREATE_FAILED,
                errorMessage = PipelineCopyTaskUtils.getErrorMessage(exception)
            )
            return null
        }
        val failedResources = listPipelineDependentResources(
            projectId = projectId,
            taskId = taskId,
            pipelineId = detail.pipelineId,
            resourceMap = resourceMap
        ).filter { it.status == PipelineCopyTaskResourceStatus.FAILED }
        if (failedResources.isNotEmpty()) {
            updatePipelineDetailStatus(
                projectId = projectId,
                taskId = taskId,
                pipelineId = detail.pipelineId,
                status = PipelineBatchTaskDetailStatus.FAILED,
                errorType = PipelineBatchTaskDetailErrorType.DEPENDENCY_CREATE_FAILED,
                errorMessage = PipelineCopyTaskUtils.buildDependencyFailedMessage(failedResources)
            )
            return null
        }
        return resource
    }

    private fun copyPipelineResource(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): PipelineCopyTaskResourceUpdate {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId = resource.targetResourceId
        var targetResourceName = resource.targetResourceName
        var errorMessage: PipelineBatchTaskErrorMessage? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID,
                PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID,
                PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {
                    validatePipelineTargetResource(resource = resource, copyStrategy = copyStrategy)
                    val targetResource = pipelineCopyResourceCreateService.createPipeline(
                        userId = userId,
                        projectId = projectId,
                        sourcePipelineId = resource.resourceId,
                        targetProjectId = targetProjectId,
                        targetPipelineId = targetResourceId!!,
                        targetPipelineName = targetResourceName!!,
                        resourceMap = resourceMap
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.PIPELINE_SKIP -> {
                    status = PipelineCopyTaskResourceStatus.SKIP
                }

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            logger.error(
                "execute pipeline failed|$projectId|$taskId|${resource.resourceId}|${resource.resourceName}",
                ignored
            )
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        return PipelineCopyTaskResourceUpdate(
            projectId = projectId,
            taskId = taskId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            targetResourceType = resource.resourceType,
            status = status,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
        )
    }

    private fun persistPipelineExecution(
        projectId: String,
        taskId: String,
        pipelineId: String,
        resource: PipelineCopyTaskResource,
        resourceUpdate: PipelineCopyTaskResourceUpdate,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ) {
        val updateStatus = resourceUpdate.status ?: return
        val status = when (updateStatus) {
            PipelineCopyTaskResourceStatus.SUCCESS,
            PipelineCopyTaskResourceStatus.SKIP,
            PipelineCopyTaskResourceStatus.FAILED -> updateStatus
            else -> return
        }
        val detailErrorType = if (status == PipelineCopyTaskResourceStatus.FAILED) {
            PipelineBatchTaskDetailErrorType.PIPELINE_CREATE_FAILED
        } else {
            null
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineCopyTaskResourceDao.update(
                dslContext = transactionContext,
                update = resourceUpdate
            )
            updatePipelineDetailStatus(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId,
                resourceStatus = status,
                errorType = detailErrorType,
                errorMessage = resourceUpdate.errorMessage,
                transactionContext = transactionContext
            )
        }
        val resourceKey = PipelineCopyTaskUtils.resourceKey(
            resourceType = resource.resourceType,
            resourceId = resource.resourceId
        )
        resourceMap[resourceKey] = resource.copy(
            status = status,
            targetResourceType = resource.resourceType,
            targetResourceId = resourceUpdate.targetResourceId,
            targetResourceName = resourceUpdate.targetResourceName,
            errorMessage = resourceUpdate.errorMessage
        )
    }

    private fun listPipelineDependentResources(
        projectId: String,
        taskId: String,
        pipelineId: String,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): List<PipelineCopyTaskResource> {
        val relations = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = setOf(pipelineId)
        )
        return relations.map { relation ->
            resourceMap[
                PipelineCopyTaskUtils.resourceKey(
                    resourceType = relation.resourceType,
                    resourceId = relation.resourceId
                )
            ]
                ?: throwDependencyFailed(relation.resourceType, relation.resourceId)
        }
    }

    private fun validatePipelineTargetResource(
        resource: PipelineCopyTaskResource,
        copyStrategy: PipelineCopyStrategy
    ) {
        if (resource.targetResourceId.isNullOrBlank() || resource.targetResourceName.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                params = arrayOf(resource.resourceType.name, resource.resourceName, copyStrategy.name)
            )
        }
    }

    private fun updatePipelineDetailStatus(
        projectId: String,
        taskId: String,
        pipelineId: String,
        status: PipelineBatchTaskDetailStatus,
        errorType: PipelineBatchTaskDetailErrorType? = null,
        errorMessage: PipelineBatchTaskErrorMessage? = null,
        transactionContext: DSLContext = dslContext
    ) {
        pipelineBatchTaskDetailDao.update(
            dslContext = transactionContext,
            update = PipelineBatchTaskDetailUpdate(
                projectId = projectId,
                taskId = taskId,
                pipelineId = pipelineId,
                status = status,
                change = false,
                errorType = errorType,
                errorMessage = errorMessage
            )
        )
    }

    private fun updatePipelineDetailStatus(
        projectId: String,
        taskId: String,
        pipelineId: String,
        resourceStatus: PipelineCopyTaskResourceStatus,
        errorType: PipelineBatchTaskDetailErrorType? = null,
        errorMessage: PipelineBatchTaskErrorMessage? = null,
        transactionContext: DSLContext = dslContext
    ) {
        val detailStatus = when (resourceStatus) {
            PipelineCopyTaskResourceStatus.SUCCESS -> PipelineBatchTaskDetailStatus.SUCCESS
            PipelineCopyTaskResourceStatus.SKIP -> PipelineBatchTaskDetailStatus.SUCCESS
            PipelineCopyTaskResourceStatus.FAILED -> PipelineBatchTaskDetailStatus.FAILED
            else -> return
        }
        updatePipelineDetailStatus(
            projectId = projectId,
            taskId = taskId,
            pipelineId = pipelineId,
            status = detailStatus,
            errorType = errorType,
            errorMessage = errorMessage,
            transactionContext = transactionContext
        )
    }

    private fun throwDependencyFailed(
        resourceType: PipelineDependentResourceType,
        resourceName: String
    ): Nothing {
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_FAILED,
            params = arrayOf("${resourceType.name}:$resourceName")
        )
    }

    private fun finishExecute(
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
            val resources = pipelineCopyTaskResourceDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId
            )
            val details = pipelineBatchTaskDetailDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId
            )
            val copyDetails = details.filter {
                it.status != PipelineBatchTaskDetailStatus.EXCLUDED
            }
            val successCount = copyDetails.count {
                it.status == PipelineBatchTaskDetailStatus.SUCCESS
            }
            val failedCount = copyDetails.count {
                it.status == PipelineBatchTaskDetailStatus.FAILED
            }
            pipelineBatchTaskDao.update(
                dslContext = dslContext,
                update = PipelineBatchTaskUpdate(
                    projectId = projectId,
                    taskId = taskId,
                    taskSummary = JsonUtil.toJson(
                        PipelineCopyTaskUtils.buildSummary(resources),
                        formatted = false
                    ),
                    status = buildExecuteStatus(
                        resourceCount = copyDetails.size,
                        successCount = successCount,
                        failedCount = failedCount
                    ),
                    successCount = successCount,
                    failedCount = failedCount,
                    clearErrorMessage = true
                )
            )
        } finally {
            lock.unlock()
        }
    }

    private fun buildExecuteStatus(
        resourceCount: Int,
        successCount: Int,
        failedCount: Int
    ): PipelineBatchTaskStatus {
        val resultCount = successCount + failedCount
        return when {
            resultCount == 0 -> PipelineBatchTaskStatus.DRAFT
            failedCount == 0 && successCount == resourceCount -> PipelineBatchTaskStatus.SUCCESS
            successCount == 0 -> PipelineBatchTaskStatus.FAILED
            else -> PipelineBatchTaskStatus.PARTIAL_FAILED
        }
    }

    private fun validateCopyStrategy(resource: PipelineCopyTaskResource): PipelineCopyStrategy {
        val copyStrategy = resource.copyStrategy ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_CAN_NOT_EMPTY,
            params = arrayOf(resource.resourceType.name, resource.resourceName)
        )
        if (!copyStrategy.support(resource.resourceType)) {
            throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
        }
        return copyStrategy
    }

    private fun throwStrategyNotSupport(
        resource: PipelineCopyTaskResource,
        copyStrategy: PipelineCopyStrategy
    ): Nothing {
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_STRATEGY_NOT_SUPPORT,
            params = arrayOf(
                resource.resourceType.name,
                resource.resourceName,
                copyStrategy.name
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskExecuteService::class.java)
    }
}
