package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskSummary
import com.tencent.devops.process.pojo.pipeline.task.PipelineLabelGroupCopyResourceProp
import org.jooq.DSLContext
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
    private val pipelineCopyResourceCreateService: PipelineCopyResourceCreateService
) {

    fun execute(event: PipelineBatchTaskExecuteEvent) {
        with(event) {
            logger.info("start to execute pipeline copy task|$projectId|$taskId")
            val task = tryStartExecute(projectId = projectId, taskId = taskId) ?: return
            val resources = pipelineCopyTaskResourceDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.PROCESSED
            )
            val param = parseParam(task) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
                params = arrayOf(task.taskId)
            )
            if (resources.isNotEmpty()) {
                // 凭证要在代码库之前创建
                executeCredentials(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executeRepositories(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                // 节点要在环境之前创建
                executeNodes(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executeEnvs(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executePipelineGroups(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executePipelineLabels(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executePipelineTemplates(
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
                executePipelines(
                    userId = task.creator,
                    projectId = projectId,
                    taskId = taskId,
                    targetProjectId = param.targetProjectId,
                    resources = resources
                )
            }
            finishExecute(projectId = projectId, taskId = taskId)
        }
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
            if (task.status != PipelineBatchTaskStatus.DRAFT) {
                logger.warn("pipeline batch task status not match|$projectId|$taskId|${task.status}")
                return null
            }
            val unprocessedCount = pipelineCopyTaskResourceDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineCopyTaskResourceStatus.UNPROCESSED
            )
            if (unprocessedCount > 0) {
                logger.warn("pipeline copy task has unprocessed resources|$projectId|$taskId|$unprocessedCount")
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
        resources.filter {
            it.resourceType == PipelineDependentResourceType.CREDENTIAL
        }.forEach {
            executeCredential(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executeCredential(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
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
                    if (resource.targetResourceId.isNullOrBlank()) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                            params = arrayOf(resource.resourceName, resource.resourceType.name, copyStrategy.name)
                        )
                    }
                    val targetResource = pipelineCopyResourceGetService.getCredentialBasicInfo(
                        userId = userId,
                        projectId = targetProjectId,
                        credentialId = resource.targetResourceId!!,
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
                            resource.resourceName,
                            copyStrategy.name,
                            resource.resourceType.name
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        resources.filter {
            it.resourceType == PipelineDependentResourceType.REPOSITORY
        }.forEach {
            executeRepository(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it,
                resourceMap = resourceMap
            )
        }
    }

    private fun executeRepository(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
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
                            resource.resourceName,
                            copyStrategy.name,
                            resource.resourceType.name
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
    }

    private fun executeEnvs(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_ENV ||
                    it.resourceType == PipelineDependentResourceType.DEPLOY_ENV
        }.forEach {
            executeEnv(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executeEnv(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
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

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
    }

    private fun executeNodes(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.BUILD_NODE ||
                    it.resourceType == PipelineDependentResourceType.DEPLOY_NODE
        }.forEach {
            executeNode(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executeNode(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME,
                PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME -> {
                    val targetResource = pipelineCopyResourceGetService.getNodeByName(
                        userId = userId,
                        projectId = targetProjectId,
                        nodeName = resource.resourceName,
                        expectExists = true
                    )
                    targetResourceId = targetResource!!.resourceId
                    targetResourceName = targetResource.resourceName
                }

                PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT,
                PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT -> {
                    val targetResource = pipelineCopyResourceCreateService.moveNodeToTargetProject(
                        userId = userId,
                        sourceProjectId = projectId,
                        nodeHashId = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = targetResource.resourceId
                    targetResourceName = targetResource.resourceName
                }

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
    }

    private fun executePipelineGroups(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP
        }.forEach {
            executePipelineGroup(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executePipelineGroup(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
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
                        viewName = resource.resourceId,
                        targetProjectId = targetProjectId
                    )
                    targetResourceId = pipelineGroup.resourceId
                    targetResourceName = pipelineGroup.resourceName
                }

                PipelineCopyStrategy.PIPELINE_GROUP_IGNORE -> Unit

                else -> throwStrategyNotSupport(resource = resource, copyStrategy = copyStrategy)
            }
        } catch (ignored: Exception) {
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
    }

    private fun executePipelineLabels(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_LABEL
        }.forEach {
            executePipelineLabel(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executePipelineLabel(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId: String? = null
        var targetResourceName: String? = null
        var errorMessage: String? = null
        try {
            val copyStrategy = validateCopyStrategy(resource)
            when (copyStrategy) {
                PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE -> {
                    val labelGroupProp = resource.resourceProperties as? PipelineLabelGroupCopyResourceProp
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
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = PipelineCopyTaskUtils.getErrorMessage(ignored)
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
    }

    private fun executePipelineTemplates(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_TEMPLATE
        }.forEach {
            executePipelineTemplate(
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it
            )
        }
    }

    private fun executePipelineTemplate(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource
    ) {
        when (resource.copyStrategy!!) {
            PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME -> {

            }

            PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW -> {

            }

            else -> {
                logger.error("unknown pipeline template copy strategy: ${resource.copyStrategy}")
            }
        }
    }

    private fun executePipelines(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: List<PipelineCopyTaskResource>
    ) {
        val resourceMap = resources.associateBy {
            PipelineCopyTaskUtils.resourceKey(resourceType = it.resourceType, resourceId = it.resourceId)
        }.toMutableMap()
        resources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE
        }.forEach {
            val result = executePipeline(
                userId = userId,
                projectId = projectId,
                taskId = taskId,
                targetProjectId = targetProjectId,
                resource = it,
                resourceMap = resourceMap
            )
            val resourceKey = PipelineCopyTaskUtils.resourceKey(
                resourceType = result.resourceType,
                resourceId = result.resourceId
            )
            resourceMap[resourceKey] = result
        }
    }

    private fun executePipeline(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineCopyTaskResource,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): PipelineCopyTaskResource {
        var status = PipelineCopyTaskResourceStatus.SUCCESS
        var targetResourceId = resource.targetResourceId
        var targetResourceName = resource.targetResourceName
        var errorMessage: String? = null
        try {
            when (val copyStrategy = validateCopyStrategy(resource)) {
                PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID,
                PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID,
                PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT -> {
                    validatePipelineTargetResource(resource = resource, copyStrategy = copyStrategy)
                    val dependentResources = listPipelineDependentResources(
                        projectId = projectId,
                        taskId = taskId,
                        pipelineId = resource.resourceId,
                        resourceMap = resourceMap
                    )
                    validatePipelineDependencies(resources = dependentResources)
                    val targetResource = pipelineCopyResourceCreateService.createPipeline(
                        userId = userId,
                        sourceProjectId = projectId,
                        sourcePipelineId = resource.resourceId,
                        targetProjectId = targetProjectId,
                        targetPipelineId = targetResourceId!!,
                        targetPipelineName = targetResourceName!!,
                        dependentResources = dependentResources
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
            status = PipelineCopyTaskResourceStatus.FAILED
            errorMessage = ignored.message
        }
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
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
        )
        updatePipelineDetailStatus(
            projectId = projectId,
            taskId = taskId,
            pipelineId = resource.resourceId,
            status = status
        )
        return resource.copy(
            status = status,
            targetResourceType = resource.resourceType,
            targetResourceId = targetResourceId,
            targetResourceName = targetResourceName,
            errorMessage = errorMessage
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

    private fun validatePipelineDependencies(resources: List<PipelineCopyTaskResource>) {
        val failedResource = resources.firstOrNull {
            it.status == PipelineCopyTaskResourceStatus.FAILED
        }
        if (failedResource != null) {
            throwDependencyFailed(failedResource.resourceType, failedResource.resourceName)
        }
    }

    private fun validatePipelineTargetResource(
        resource: PipelineCopyTaskResource,
        copyStrategy: PipelineCopyStrategy
    ) {
        if (resource.targetResourceId.isNullOrBlank() || resource.targetResourceName.isNullOrBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_EMPTY,
                params = arrayOf(resource.resourceName, resource.resourceType.name, copyStrategy.name)
            )
        }
    }

    private fun updatePipelineDetailStatus(
        projectId: String,
        taskId: String,
        pipelineId: String,
        status: PipelineCopyTaskResourceStatus
    ) {
        val detailStatus = when (status) {
            PipelineCopyTaskResourceStatus.SUCCESS -> PipelineBatchTaskDetailStatus.SUCCESS
            PipelineCopyTaskResourceStatus.FAILED -> PipelineBatchTaskDetailStatus.FAILED
            else -> return
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = setOf(pipelineId),
            status = detailStatus,
            change = false
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
            val successCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.SUCCESS ||
                    it.status == PipelineCopyTaskResourceStatus.SKIP
            }
            val failedCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.FAILED
            }
            pipelineBatchTaskDao.update(
                dslContext = dslContext,
                update = PipelineBatchTaskUpdate(
                    projectId = projectId,
                    taskId = taskId,
                    taskSummary = JsonUtil.toJson(buildSummary(resources), formatted = false),
                    status = buildExecuteStatus(
                        resourceCount = resources.size,
                        successCount = successCount,
                        failedCount = failedCount
                    ),
                    successCount = successCount,
                    failedCount = failedCount
                )
            )
        } finally {
            lock.unlock()
        }
    }

    private fun buildSummary(resources: List<PipelineCopyTaskResource>): PipelineCopyTaskSummary {
        return PipelineCopyTaskSummary(
            unprocessedCount = resources.count {
                it.status == PipelineCopyTaskResourceStatus.UNPROCESSED
            },
            highRiskCount = resources.count { it.highRisk },
            needCompletionCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_COMPLETION
            },
            needTransferCount = resources.count {
                it.copyAction == PipelineCopyAction.NEED_TRANSFER
            },
            autoFinishCount = resources.count {
                it.copyAction == PipelineCopyAction.AUTO_FINISH
            }
        )
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
                resource.resourceName,
                copyStrategy.name,
                resource.resourceType.name
            )
        )
    }

    private fun parseParam(task: PipelineBatchTask): PipelineBatchCopyTaskParam? {
        return task.taskParam?.takeIf { it.isNotBlank() }?.let {
            JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskExecuteService::class.java)
    }
}
