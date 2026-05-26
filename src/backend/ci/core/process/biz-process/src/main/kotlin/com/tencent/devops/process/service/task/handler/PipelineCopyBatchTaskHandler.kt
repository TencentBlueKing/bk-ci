package com.tencent.devops.process.service.task.handler

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeSource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskConfigRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskCreateRequest
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetailUpdate
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskExecuteEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskInfo
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceUpdate
import com.tencent.devops.process.pojo.pipeline.task.RepositoryCopyResourceProperties
import com.tencent.devops.process.service.PipelineDependentResourceService
import com.tencent.devops.process.service.task.PipelineBatchTaskHandler
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineCopyBatchTaskHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineDependentResourceService: PipelineDependentResourceService
) : PipelineBatchTaskHandler {

    override fun support(taskType: PipelineBatchTaskType): Boolean = taskType == PipelineBatchTaskType.PIPELINE_COPY

    override fun taskStatusWhenCreate(): PipelineBatchTaskStatus {
        return PipelineBatchTaskStatus.COPY_SUB_PIPELINE_ANALYZING
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

    override fun create(event: PipelineBatchTaskCreateEvent) {
        getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        val details = pipelineBatchTaskDetailDao.listByTaskId(
            dslContext = dslContext,
            projectId = event.projectId,
            taskId = event.taskId
        )
        val subPipelineResources = details
            .filterNot { it.subPipeline }
            .flatMap { detail ->
                pipelineDependentResourceService.analysisSubPipelineDependency(
                    projectId = event.projectId,
                    pipelineId = detail.pipelineId
                )
            }
            .filter { it.resourceType == PipelineDependentResourceType.PIPELINE }
            .distinctBy { it.resourceId }
        val existsPipelineIds = details.map { it.pipelineId }.toSet()
        val subPipelineIds = subPipelineResources.map { it.resourceId }
        val pacPipelineIds = pipelineYamlInfoDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = event.projectId,
            pipelineIds = subPipelineIds
        ).map { it.pipelineId }.toSet()
        val draftPipelineIds = subPipelineIds.filter {
            pipelineRepositoryService.getDraftVersionResource(projectId = event.projectId, pipelineId = it) != null
        }.toSet()
        val constraintPipelineIds = pipelineTemplateRelatedService.listByPipelineIds(
            projectId = event.projectId,
            pipelineIds = subPipelineIds.toSet()
        ).filter { it.instanceType == PipelineInstanceTypeEnum.CONSTRAINT }
            .map { it.pipelineId }
            .toSet()
        val subDetails = subPipelineResources
            .filterNot { existsPipelineIds.contains(it.resourceId) }
            .map { resource ->
                PipelineBatchTaskDetailInfo(
                    taskId = event.taskId,
                    projectId = event.projectId,
                    taskType = event.taskType,
                    pipelineId = resource.resourceId,
                    pipelineName = resource.resourceName,
                    pac = pacPipelineIds.contains(resource.resourceId),
                    constraint = constraintPipelineIds.contains(resource.resourceId),
                    subPipeline = true,
                    draft = draftPipelineIds.contains(resource.resourceId),
                    status = PipelineBatchTaskDetailStatus.WAIT_COPY,
                    errorMessage = null,
                    startTime = null,
                    endTime = null
                )
            }
        val allDetails = mutableListOf<PipelineBatchTaskDetailInfo>().apply {
            addAll(details)
            addAll(subDetails)
        }
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineBatchTaskDetailDao.batchCreate(
                dslContext = transactionContext,
                details = subDetails
            )
            pipelineCopyTaskResourceDao.batchCreate(
                dslContext = transactionContext,
                resources = allDetails.map(::buildPipelineCopyResource)
            )
            pipelineBatchTaskDao.updateStatus(
                dslContext = transactionContext,
                projectId = event.projectId,
                taskId = event.taskId,
                status = PipelineBatchTaskStatus.COPY_SUB_PIPELINE_ANALYSIS_FINISHED
            )
        }
    }

    override fun validateWhenConfig(
        userId: String,
        projectId: String,
        task: PipelineBatchTaskInfo,
        request: PipelineBatchTaskConfigRequest
    ) {
        if (request.taskParam == null) {
            throw InvalidParamException("taskParam cannot be null")
        }
        val param = request.taskParam as? PipelineBatchCopyTaskParam
            ?: throw InvalidParamException("taskParam must be PipelineBatchCopyTaskParam")
        param.pipelineCopyStrategy.takeIf { it.support(PipelineDependentResourceType.PIPELINE) }
            ?: throw InvalidParamException("pipelineCopyStrategy only supports PIPELINE")
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = param.targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, param.targetProjectId)
            )
        }
    }

    override fun config(event: PipelineBatchTaskConfigEvent) {
        getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        val changeCount = pipelineBatchTaskDetailDao.count(
            dslContext = dslContext,
            projectId = event.projectId,
            taskId = event.taskId,
            change = true
        )
        if (changeCount == 0L) {
            return
        }
        excludedSubPipelineTask(projectId = event.projectId, taskId = event.taskId)
        deleteExcludedCopyResources(projectId = event.projectId, taskId = event.taskId)
        createOrUpdateCopyResources(projectId = event.projectId, taskId = event.taskId)
    }

    override fun validateWhenExecute(userId: String, projectId: String, task: PipelineBatchTaskInfo) {
        val param = getParam(task)
        if (!pipelinePermissionService.checkProjectManager(userId = userId, projectId = param.targetProjectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, param.targetProjectId)
            )
        }
        val unfinished = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = task.taskId
        ).filter { it.status == PipelineCopyTaskResourceStatus.UNPROCESSED }
        if (unfinished.isNotEmpty()) {
            throw InvalidParamException("copy resources must be processed before execute")
        }
    }

    override fun execute(event: PipelineBatchTaskExecuteEvent) {
        val task = getTask(projectId = event.projectId, taskId = event.taskId) ?: return
        val resources = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = event.projectId,
            taskId = event.taskId
        ).filter { it.status == PipelineCopyTaskResourceStatus.PROCESSED }
        resources.sortedWith(compareBy { RESOURCE_ORDER[it.resourceType] ?: Int.MAX_VALUE })
            .forEach { detail ->
                try {
                    executeResource(task = task, detail = detail)
                } catch (ignored: Exception) {
                    logger.warn(
                        "copy resource failed|projectId=${event.projectId}|taskId=${event.taskId}|" +
                            "resourceType=${detail.resourceType}|resourceId=${detail.resourceId}",
                        ignored
                    )
                    pipelineCopyTaskResourceDao.update(
                        dslContext = dslContext,
                        update = PipelineCopyTaskResourceUpdate(
                            projectId = detail.projectId,
                            taskId = detail.taskId,
                            resourceType = detail.resourceType,
                            resourceId = detail.resourceId,
                            status = PipelineCopyTaskResourceStatus.FAILED,
                            errorMessage = ignored.message
                        )
                    )
                }
            }
    }

    private fun buildPipelineCopyResource(detail: PipelineBatchTaskDetailInfo): PipelineCopyTaskResourceInfo {
        return PipelineCopyTaskResourceInfo(
            taskId = detail.taskId,
            projectId = detail.projectId,
            pipelineId = detail.pipelineId,
            resourceType = PipelineDependentResourceType.PIPELINE,
            resourceId = detail.pipelineId,
            resourceName = detail.pipelineName,
            status = PipelineCopyTaskResourceStatus.PROCESSED
        )
    }

    private fun buildResourceCopyResources(
        userId: String,
        task: PipelineBatchTaskInfo,
        param: PipelineBatchCopyTaskParam,
        detail: PipelineBatchTaskDetailInfo
    ): List<PipelineCopyTaskResourceInfo> {
        return pipelineDependentResourceService.analysisResourceDependency(
            userId = userId,
            projectId = task.projectId,
            pipelineId = detail.pipelineId
        ).map { resource ->
            PipelineCopyTaskResourceInfo(
                taskId = task.taskId,
                projectId = task.projectId,
                pipelineId = detail.pipelineId,
                resourceType = resource.resourceType,
                resourceId = resource.resourceId,
                resourceName = resource.resourceName,
                resourceProperties = buildResourceProperties(resource),
                copyStrategy = defaultStrategy(resource.resourceType),
                targetProjectId = param.targetProjectId,
                status = PipelineCopyTaskResourceStatus.PROCESSED,
                targetNameExists = targetNameExists(
                    userId = userId,
                    targetProjectId = param.targetProjectId,
                    resource = resource
                )
            )
        }
    }

    private fun buildPipelineResourceCopyResources(
        task: PipelineBatchTaskInfo,
        param: PipelineBatchCopyTaskParam,
        detail: PipelineBatchTaskDetailInfo
    ): List<PipelineCopyTaskResourceInfo> {
        val resources = mutableListOf(
            PipelineDependentResource(
                projectId = task.projectId,
                resourceType = PipelineDependentResourceType.PIPELINE,
                resourceId = detail.pipelineId,
                resourceName = detail.pipelineName
            )
        )
        resources.addAll(
            pipelineDependentResourceService.analysisSubPipelineDependency(
                projectId = task.projectId,
                pipelineId = detail.pipelineId
            )
        )
        return resources.map { resource ->
            PipelineCopyTaskResourceInfo(
                taskId = task.taskId,
                projectId = task.projectId,
                pipelineId = detail.pipelineId,
                resourceType = PipelineDependentResourceType.PIPELINE,
                resourceId = resource.resourceId,
                resourceName = resource.resourceName,
                copyStrategy = param.pipelineCopyStrategy,
                targetProjectId = param.targetProjectId,
                status = PipelineCopyTaskResourceStatus.PROCESSED,
                targetNameExists = targetNameExists(
                    userId = task.creator,
                    targetProjectId = param.targetProjectId,
                    resource = resource
                )
            )
        }
    }

    /**
     * 排查子流水线任务
     *
     * 当流水线被排除后,需要把关联的子流水线任务也排除,只有当子流水线仅被当前排除的流水线引用时,才能排除
     */
    private fun excludedSubPipelineTask(
        projectId: String,
        taskId: String
    ) {
        // 获取排除的任务详情
        val taskDetails = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            subPipeline = false,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        )
        if (taskDetails.isEmpty()) {
            return
        }
        val excludedPipelineIds = taskDetails.map { it.pipelineId }.toSet()
        // 获取排除流水线依赖的子流水线
        val resourceIds = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludedPipelineIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).map { it.resourceId }.toSet()
        if (resourceIds.isEmpty()) {
            return
        }
        // 子流水线除了被排除的流水线引用,还有没有被其他流水线引用,如果还有被其他流水线引用,则不排除
        val activeResourceCountMap = pipelineCopyTaskResourceDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = resourceIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).filterNot { excludedPipelineIds.contains(it.pipelineId) || it.pipelineId == it.resourceId }
            .groupingBy { it.resourceId }
            .eachCount()
        val subPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = resourceIds,
            subPipeline = true
        ).map { it.pipelineId }.toSet()
        val excludeSubPipelineIds = resourceIds.filter {
            subPipelineIds.contains(it) && (activeResourceCountMap[it] ?: 0) == 0
        }.toSet()
        if (excludeSubPipelineIds.isEmpty()) {
            return
        }
        pipelineBatchTaskDetailDao.update(
            dslContext = dslContext,
            update = PipelineBatchTaskDetailUpdate(
                projectId = projectId,
                taskId = taskId,
                pipelineIds = excludeSubPipelineIds,
                status = PipelineBatchTaskDetailStatus.EXCLUDED,
                change = true
            )
        )
    }

    /**
     * 删除已排除流水线复制资源
     *
     * 删除任务中排除的流水线复制资源
     */
    private fun deleteExcludedCopyResources(
        projectId: String,
        taskId: String
    ) {
        // 获取排除的任务详情
        val taskDetails = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            subPipeline = false,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        )
        if (taskDetails.isEmpty()) {
            return
        }
        pipelineCopyTaskResourceDao.deleteByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = taskDetails.map { it.pipelineId }.toSet()
        )
    }

    /**
     * 创建或更新复制资源
     */
    private fun createOrUpdateCopyResources(
        projectId: String,
        taskId: String
    ) {

    }

    private fun executeResource(task: PipelineBatchTaskInfo, detail: PipelineCopyTaskResourceInfo) {
        when (detail.resourceType) {
            PipelineDependentResourceType.CREDENTIAL -> executeCredential(task, detail)
            PipelineDependentResourceType.REPOSITORY -> executeRepository(task, detail)
            PipelineDependentResourceType.BUILD_NODE,
            PipelineDependentResourceType.DEPLOY_NODE -> executeNode(task, detail)
            PipelineDependentResourceType.ENVIRONMENT -> executeEnvironment(task, detail)
            PipelineDependentResourceType.PIPELINE_LABEL,
            PipelineDependentResourceType.PIPELINE_GROUP -> autoFinish(detail)
            PipelineDependentResourceType.PIPELINE -> copySuccess(detail)
            PipelineDependentResourceType.PIPELINE_TEMPLATE -> copySuccess(detail)
        }
    }

    private fun executeCredential(task: PipelineBatchTaskInfo, detail: PipelineCopyTaskResourceInfo) {
        val param = getParam(task)
        val credential = client.get(ServiceCredentialResource::class).getBasicInfo(
            userId = task.creator,
            projectId = task.projectId,
            credentialId = detail.resourceId
        ).data ?: throw InvalidParamException("credential ${detail.resourceId} not found")
        val targetCredential = client.get(ServiceCredentialResource::class).list(
            projectId = param.targetProjectId,
            page = 1,
            pageSize = 1000
        ).data?.records?.firstOrNull { it.credentialName == detail.resourceName || it.credentialId == detail.resourceId }
        if (detail.copyStrategy == PipelineCopyStrategy.CREDENTIAL_CREATE_NEW) {
            throw InvalidParamException("credential secret value is required to create new credential")
        }
        val targetId = targetCredential?.credentialId ?: detail.targetResourceId
            ?: throw InvalidParamException("target credential not found")
        updateSuccess(
            detail = detail,
            targetResourceId = targetId,
            targetResourceName = targetCredential?.credentialName ?: credential.credentialName ?: detail.resourceName,
            status = PipelineCopyTaskResourceStatus.SUCCESS
        )
    }

    private fun executeRepository(task: PipelineBatchTaskInfo, detail: PipelineCopyTaskResourceInfo) {
        val param = getParam(task)
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = task.projectId,
            repositoryId = detail.resourceId,
            repositoryType = RepositoryType.ID
        ).data ?: throw InvalidParamException("repository ${detail.resourceId} not found")
        if (detail.copyStrategy == PipelineCopyStrategy.REPOSITORY_CREATE_NEW) {
            val targetId = client.get(ServiceRepositoryResource::class).create(
                userId = task.creator,
                projectId = param.targetProjectId,
                repository = repository
            ).data?.hashId ?: throw InvalidParamException("create repository failed")
            updateSuccess(
                detail = detail,
                targetResourceId = targetId,
                targetResourceName = repository.aliasName,
                targetResourceProperties = repositoryProperties(repository),
                status = PipelineCopyTaskResourceStatus.AUTO_FINISHED
            )
            return
        }
        val targetRepository = client.get(ServiceRepositoryResource::class).list(
            projectId = param.targetProjectId,
            repositoryType = null
        ).data?.firstOrNull { it.aliasName == detail.resourceName && it.type == repository.getScmType() }
            ?: throw InvalidParamException("target repository ${detail.resourceName} not found")
        updateSuccess(
            detail = detail,
            targetResourceId = targetRepository.repositoryHashId,
            targetResourceName = targetRepository.aliasName,
            targetResourceProperties = RepositoryCopyResourceProperties(
                authType = null,
                authInfo = null,
                repositoryType = targetRepository.type.name,
                repositoryUrl = targetRepository.url
            ),
            status = PipelineCopyTaskResourceStatus.SUCCESS
        )
    }

    private fun executeNode(task: PipelineBatchTaskInfo, detail: PipelineCopyTaskResourceInfo) {
        val param = getParam(task)
        if (detail.copyStrategy == PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT ||
            detail.copyStrategy == PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT
        ) {
            client.get(ServiceNodeResource::class).transferNodes(
                userId = task.creator,
                projectId = task.projectId,
                targetProjectId = param.targetProjectId,
                nodeHashIds = listOf(detail.resourceId)
            )
            updateSuccess(
                detail = detail,
                targetResourceId = detail.resourceId,
                targetResourceName = detail.resourceName,
                status = PipelineCopyTaskResourceStatus.TRANSFER_TODO
            )
            return
        }
        val targetNode = client.get(ServiceNodeResource::class).getNodeStatus(
            userId = task.creator,
            projectId = param.targetProjectId,
            nodeHashId = null,
            nodeName = detail.resourceName,
            agentHashId = null
        ).data ?: throw InvalidParamException("target node ${detail.resourceName} not found")
        updateSuccess(
            detail = detail,
            targetResourceId = targetNode.nodeHashId,
            targetResourceName = targetNode.displayName,
            status = PipelineCopyTaskResourceStatus.SUCCESS
        )
    }

    private fun executeEnvironment(task: PipelineBatchTaskInfo, detail: PipelineCopyTaskResourceInfo) {
        val param = getParam(task)
        val environmentResource = client.get(ServiceEnvironmentResource::class)
        if (detail.copyStrategy == PipelineCopyStrategy.ENVIRONMENT_REUSE_SAME_NAME) {
            val targetEnv = environmentResource.getByName(
                userId = task.creator,
                projectId = param.targetProjectId,
                envName = detail.resourceName,
                checkPermission = false
            ).data ?: throw InvalidParamException("target environment ${detail.resourceName} not found")
            updateSuccess(
                detail = detail,
                targetResourceId = targetEnv.envHashId,
                targetResourceName = targetEnv.name,
                status = PipelineCopyTaskResourceStatus.SUCCESS
            )
            return
        }
        val sourceEnv = environmentResource.get(
            userId = task.creator,
            projectId = task.projectId,
            envHashId = detail.resourceId,
            checkPermission = false
        ).data ?: throw InvalidParamException("environment ${detail.resourceId} not found")
        val targetEnvId = environmentResource.create(
            userId = task.creator,
            projectId = param.targetProjectId,
            environment = EnvCreateInfo(
                name = sourceEnv.name,
                desc = sourceEnv.desc,
                envType = EnvType.valueOf(sourceEnv.envType),
                envVars = sourceEnv.envVars,
                source = NodeSource.EXISTING,
                nodeHashIds = emptyList()
            )
        ).data?.hashId ?: throw InvalidParamException("create environment failed")
        updateSuccess(
            detail = detail,
            targetResourceId = targetEnvId,
            targetResourceName = sourceEnv.name,
            status = if (detail.copyStrategy == PipelineCopyStrategy.ENVIRONMENT_CREATE_WITHOUT_NODE) {
                PipelineCopyTaskResourceStatus.PENDING_COMPLETION
            } else {
                PipelineCopyTaskResourceStatus.AUTO_FINISHED
            }
        )
    }

    private fun targetNameExists(
        userId: String,
        targetProjectId: String,
        resource: PipelineDependentResource
    ): Boolean {
        return runCatching {
            when (resource.resourceType) {
                PipelineDependentResourceType.PIPELINE -> pipelineInfoDao.getPipelineInfoByName(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    pipelineName = resource.resourceName
                ) != null
                PipelineDependentResourceType.ENVIRONMENT -> client.get(ServiceEnvironmentResource::class).getByName(
                    userId = userId,
                    projectId = targetProjectId,
                    envName = resource.resourceName,
                    checkPermission = false
                ).data != null
                PipelineDependentResourceType.BUILD_NODE,
                PipelineDependentResourceType.DEPLOY_NODE -> client.get(ServiceNodeResource::class).getNodeStatus(
                    userId = userId,
                    projectId = targetProjectId,
                    nodeHashId = null,
                    nodeName = resource.resourceName,
                    agentHashId = null
                ).data != null
                PipelineDependentResourceType.CREDENTIAL -> client.get(ServiceCredentialResource::class).list(
                    projectId = targetProjectId,
                    page = 1,
                    pageSize = 1000
                ).data?.records?.any { it.credentialName == resource.resourceName } == true
                PipelineDependentResourceType.REPOSITORY -> client.get(ServiceRepositoryResource::class).list(
                    projectId = targetProjectId,
                    repositoryType = null
                ).data?.any { it.aliasName == resource.resourceName } == true
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun defaultStrategy(resourceType: PipelineDependentResourceType): PipelineCopyStrategy? {
        return when (resourceType) {
            PipelineDependentResourceType.REPOSITORY -> PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL
            PipelineDependentResourceType.ENVIRONMENT -> PipelineCopyStrategy.ENVIRONMENT_REUSE_SAME_NAME
            PipelineDependentResourceType.BUILD_NODE -> PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME
            PipelineDependentResourceType.DEPLOY_NODE -> PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME
            PipelineDependentResourceType.CREDENTIAL -> PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME
            PipelineDependentResourceType.PIPELINE_LABEL -> PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE
            PipelineDependentResourceType.PIPELINE_GROUP -> PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE
            PipelineDependentResourceType.PIPELINE -> PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID
            PipelineDependentResourceType.PIPELINE_TEMPLATE -> null
        }
    }

    private fun buildResourceProperties(resource: PipelineDependentResource): RepositoryCopyResourceProperties? {
        if (resource.resourceType != PipelineDependentResourceType.REPOSITORY) {
            return null
        }
        return runCatching {
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = resource.projectId,
                repositoryId = resource.resourceId,
                repositoryType = RepositoryType.ID
            ).data
            repository?.let(::repositoryProperties)
        }.getOrNull()
    }

    private fun repositoryProperties(repository: Repository): RepositoryCopyResourceProperties {
        return RepositoryCopyResourceProperties(
            authType = null,
            authInfo = repository.credentialId,
            repositoryType = repository.getScmType().name,
            repositoryUrl = repository.url
        )
    }

    private fun updateSuccess(
        detail: PipelineCopyTaskResourceInfo,
        targetResourceId: String?,
        targetResourceName: String?,
        targetResourceProperties: com.tencent.devops.process.pojo.pipeline.task.PipelineCopyResourceProperties? = null,
        status: PipelineCopyTaskResourceStatus
    ) {
        pipelineCopyTaskResourceDao.update(
            dslContext = dslContext,
            update = PipelineCopyTaskResourceUpdate(
                projectId = detail.projectId,
                taskId = detail.taskId,
                resourceType = detail.resourceType,
                resourceId = detail.resourceId,
                targetResourceType = detail.resourceType,
                targetResourceId = targetResourceId,
                targetResourceName = targetResourceName,
                targetResourceProperties = targetResourceProperties,
                status = status
            )
        )
    }

    private fun autoFinish(detail: PipelineCopyTaskResourceInfo) {
        updateSuccess(
            detail = detail,
            targetResourceId = detail.targetResourceId,
            targetResourceName = detail.targetResourceName ?: detail.resourceName,
            status = PipelineCopyTaskResourceStatus.AUTO_FINISHED
        )
    }

    private fun copySuccess(detail: PipelineCopyTaskResourceInfo) {
        updateSuccess(
            detail = detail,
            targetResourceId = detail.targetResourceId ?: detail.resourceId,
            targetResourceName = detail.targetResourceName ?: detail.resourceName,
            status = PipelineCopyTaskResourceStatus.SUCCESS
        )
    }

    private fun getTask(projectId: String, taskId: String): PipelineBatchTaskInfo? {
        return pipelineBatchTaskDao.get(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId
        ) ?: run {
            logger.warn(
                "pipeline copy batch task not found when create event received|projectId=$projectId|taskId=taskId"
            )
            return null
        }
    }

    private fun getParam(task: PipelineBatchTaskInfo): PipelineBatchCopyTaskParam {
        return task.taskParam?.let { JsonUtil.to(it, PipelineBatchCopyTaskParam::class.java) }
            ?: throw InvalidParamException("taskParam must be PipelineBatchCopyTaskParam")
    }

    private fun <T> Result<T>.requireData(message: String): T {
        return data ?: throw InvalidParamException(message)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyBatchTaskHandler::class.java)
        private val RESOURCE_ORDER = mapOf(
            PipelineDependentResourceType.CREDENTIAL to 1,
            PipelineDependentResourceType.REPOSITORY to 2,
            PipelineDependentResourceType.BUILD_NODE to 3,
            PipelineDependentResourceType.DEPLOY_NODE to 3,
            PipelineDependentResourceType.ENVIRONMENT to 4,
            PipelineDependentResourceType.PIPELINE_LABEL to 5,
            PipelineDependentResourceType.PIPELINE_GROUP to 5,
            PipelineDependentResourceType.PIPELINE to 6
        )
    }
}
