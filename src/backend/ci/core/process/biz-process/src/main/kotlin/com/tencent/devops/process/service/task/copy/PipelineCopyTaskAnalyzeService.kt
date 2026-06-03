package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineBatchTaskDao
import com.tencent.devops.process.dao.PipelineBatchTaskDetailDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceDao
import com.tencent.devops.process.dao.PipelineCopyTaskResourceRelDao
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.template.PipelineTemplateInfoDao
import com.tencent.devops.process.engine.cfg.PipelineIdGenerator
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchCopyTaskParam
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTask
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskAnalyzeEvent
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.task.PipelineConflictCopyResourceProp
import com.tencent.devops.process.pojo.pipeline.task.PipelineConflictInfo
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResourceRel
import com.tencent.devops.process.pojo.pipeline.task.PipelineLabelGroupCopyResourceProp
import com.tencent.devops.process.pojo.pipeline.task.PipelineViewCopyResourceProp
import com.tencent.devops.process.pojo.pipeline.task.RepoAuthCopyResourceProp
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.GithubRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.ScmGitRepository
import com.tencent.devops.repository.pojo.ScmSvnRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线复制任务分析服务
 */
@Service
class PipelineCopyTaskAnalyzeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineBatchTaskDetailDao: PipelineBatchTaskDetailDao,
    private val pipelineCopyTaskResourceDao: PipelineCopyTaskResourceDao,
    private val pipelineCopyTaskResourceRelDao: PipelineCopyTaskResourceRelDao,
    private val pipelineDependencyAnalyzeService: PipelineDependencyAnalyzeService,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineTemplateInfoDao: PipelineTemplateInfoDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val pipelineBatchTaskDao: PipelineBatchTaskDao,
    private val pipelineCopyTaskStateService: PipelineCopyTaskStateService,
    private val pipelineIdGenerator: PipelineIdGenerator
) {
    fun analyze(event: PipelineBatchTaskAnalyzeEvent) {
        with(event) {
            logger.info("start to analyze pipeline copy task|$projectId|$taskId")
            val task = tryStartAnalyze(
                projectId = projectId,
                taskId = taskId
            ) ?: return
            doAnalyze(
                projectId = event.projectId,
                task = task
            )
            finishAnalyze(
                projectId = event.projectId,
                taskId = taskId
            )
        }
    }

    private fun doAnalyze(
        projectId: String,
        task: PipelineBatchTask
    ) {
        val param = parseParam(task) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TASK_CONFIG_NOT_EXISTS,
            params = arrayOf(task.taskId)
        )
        excludeSubPipelineTasks(
            projectId = projectId,
            taskId = task.taskId
        )
        restoreSubPipelineTask(
            projectId = projectId,
            taskId = task.taskId
        )
        deleteExcludedCopyResources(
            projectId = projectId,
            taskId = task.taskId
        )
        createOrUpdateCopyResources(
            userId = task.creator,
            projectId = projectId,
            taskId = task.taskId,
            targetProjectId = param.targetProjectId,
            pipelineCopyStrategy = param.pipelineCopyStrategy
        )
    }

    fun finishAnalyze(
        projectId: String,
        taskId: String
    ) {
        // 分析完成后,把状态再转换成草稿
        pipelineCopyTaskStateService.updateTaskStatusWithLock(
            projectId = projectId,
            taskId = taskId,
            status = PipelineBatchTaskStatus.DRAFT
        )
    }

    /**
     * 排除子流水线任务
     *
     * 当流水线被排除后,需要把关联的子流水线任务也排除,只有当子流水线仅被当前排除的流水线引用时,才能排除
     */
    private fun excludeSubPipelineTasks(
        projectId: String,
        taskId: String
    ) {
        // 获取用户手动排除的流水线,作为自动排除子流水线的起点
        val rootPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            subPipeline = false,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        ).map { it.pipelineId }.toSet()
        if (rootPipelineIds.isEmpty()) {
            return
        }
        val excludedPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            status = PipelineBatchTaskDetailStatus.EXCLUDED,
        ).map { it.pipelineId }.toMutableSet()

        excludeSubPipelineTasks(
            projectId = projectId,
            taskId = taskId,
            pipelineIds = rootPipelineIds,
            excludedPipelineIds = excludedPipelineIds
        )
    }

    private fun excludeSubPipelineTasks(
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>,
        excludedPipelineIds: MutableSet<String>
    ) {
        if (pipelineIds.isEmpty()) {
            return
        }
        // 只从本轮真正排除的流水线继续向下扩散,避免中间节点未排除时误排它的子流水线
        val subPipelineIds = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = pipelineIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).map { it.resourceId }.toSet()
        if (subPipelineIds.isEmpty()) {
            return
        }
        // 获取待复制的子流水线
        val candidateSubPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = subPipelineIds,
            subPipeline = true,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY
        ).map { it.pipelineId }.toSet()
        if (candidateSubPipelineIds.isEmpty()) {
            return
        }
        val excludeSubPipelineIds = filterExcludeSubPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            subPipelineIds = candidateSubPipelineIds,
            excludedPipelineIds = excludedPipelineIds
        )
        if (excludeSubPipelineIds.isEmpty()) {
            return
        }
        // 本轮可排除的子流水线会成为下一轮起点,继续检查更深层的子流水线
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludeSubPipelineIds,
            status = PipelineBatchTaskDetailStatus.EXCLUDED,
            change = true
        )
        excludedPipelineIds.addAll(excludeSubPipelineIds)
        excludeSubPipelineTasks(
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludeSubPipelineIds,
            excludedPipelineIds = excludedPipelineIds
        )
    }

    private fun filterExcludeSubPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        subPipelineIds: Set<String>,
        excludedPipelineIds: Set<String>
    ): Set<String> {
        if (subPipelineIds.isEmpty()) {
            return emptySet()
        }
        val resourceRelations = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            resourceIds = subPipelineIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).filterNot {
            it.pipelineId == it.resourceId || excludedPipelineIds.contains(it.pipelineId)
        }
        // 候选子流水线如果仍被待复制流水线引用,不能自动排除
        val referPipelineIds = resourceRelations.map { it.pipelineId }.toSet()
        val waitCopyPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = referPipelineIds,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY
        ).map { it.pipelineId }.toSet()
        val activeResourceCountMap = resourceRelations
            .filter { waitCopyPipelineIds.contains(it.pipelineId) }
            .groupingBy { it.resourceId }
            .eachCount()
        return subPipelineIds.filter { (activeResourceCountMap[it] ?: 0) == 0 }.toSet()
    }

    /**
     * 恢复子流水线任务
     *
     * 当流水线恢复为待复制后,需要把关联的已排除子流水线任务也恢复
     */
    private fun restoreSubPipelineTask(
        projectId: String,
        taskId: String
    ) {
        val taskDetails = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY
        )
        if (taskDetails.isEmpty()) {
            return
        }
        val subPipelineIds = mutableSetOf<String>()
        collectSubPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = taskDetails.map { it.pipelineId }.toSet(),
            visited = taskDetails.map { it.pipelineId }.toMutableSet(),
            result = subPipelineIds
        )
        if (subPipelineIds.isEmpty()) {
            return
        }
        val restoreSubPipelineIds = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = subPipelineIds,
            subPipeline = true,
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        ).map { it.pipelineId }.toSet()
        if (restoreSubPipelineIds.isEmpty()) {
            return
        }
        pipelineBatchTaskDetailDao.updateStatus(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = restoreSubPipelineIds,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY,
            change = true
        )
    }

    private fun collectSubPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        taskId: String,
        pipelineIds: Set<String>,
        visited: MutableSet<String>,
        result: MutableSet<String>
    ) {
        if (pipelineIds.isEmpty()) {
            return
        }
        val subPipelineIds = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = pipelineIds,
            resourceType = PipelineDependentResourceType.PIPELINE
        ).map { it.resourceId }
            .filter { visited.add(it) }
            .toSet()
        if (subPipelineIds.isEmpty()) {
            return
        }
        result.addAll(subPipelineIds)
        collectSubPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = subPipelineIds,
            visited = visited,
            result = result
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
            status = PipelineBatchTaskDetailStatus.EXCLUDED
        )
        if (taskDetails.isEmpty()) {
            return
        }
        // 先记录受排除流水线影响的资源，删除引用后再判断这些资源是否仍被其他流水线使用
        val excludePipelineIds = taskDetails.map { it.pipelineId }.toSet()
        val affectedResourceIdsMap = pipelineCopyTaskResourceRelDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludePipelineIds
        ).groupBy({ it.resourceType }, { it.resourceId })
            .mapValues { it.value.toSet() }

        // REF 表按流水线维度删除即可，资源主表需要等确认没有任何引用后再删除
        pipelineCopyTaskResourceRelDao.deleteByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            pipelineIds = excludePipelineIds
        )
        if (affectedResourceIdsMap.isEmpty()) {
            return
        }

        // 再查一次受影响资源的剩余引用，只清理已经不被任何流水线引用的资源
        val referencedResourceIdsMap = affectedResourceIdsMap.flatMap { (resourceType, resourceIds) ->
            pipelineCopyTaskResourceRelDao.list(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                resourceIds = resourceIds,
                resourceType = resourceType
            )
        }.groupBy({ it.resourceType }, { it.resourceId })
            .mapValues { it.value.toSet() }
        // 受影响资源减去仍有引用的资源，剩下的就是可以从资源主表删除的数据
        val unreferencedResourceIdsMap = mutableMapOf<PipelineDependentResourceType, Set<String>>()
        affectedResourceIdsMap.forEach { (resourceType, resourceIds) ->
            val referencedResourceIds = referencedResourceIdsMap[resourceType].orEmpty()
            val unreferencedResourceIds = resourceIds.filterNot {
                referencedResourceIds.contains(it)
            }.toSet()
            if (unreferencedResourceIds.isNotEmpty()) {
                unreferencedResourceIdsMap[resourceType] = unreferencedResourceIds
            }
        }
        if (unreferencedResourceIdsMap.isEmpty()) {
            return
        }
        unreferencedResourceIdsMap.forEach { (resourceType, resourceIds) ->
            if (resourceIds.isEmpty()) return@forEach
            pipelineCopyTaskResourceDao.deleteByResourceIds(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                resourceType = resourceType,
                resourceIds = resourceIds
            )
        }
    }

    /**
     * 创建或更新复制资源
     */
    private fun createOrUpdateCopyResources(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        pipelineCopyStrategy: PipelineCopyStrategy
    ) {
        val details = pipelineBatchTaskDetailDao.list(
            dslContext = dslContext,
            projectId = projectId,
            taskId = taskId,
            change = true,
            status = PipelineBatchTaskDetailStatus.WAIT_COPY
        )
        if (details.isEmpty()) {
            return
        }

        val pipelineIds = details.map { it.pipelineId }.toSet()
        val copyTaskResourceRelSet = mutableSetOf<PipelineCopyTaskResourceRel>()
        val resources = details.flatMap { detail ->
            val dependentResources = pipelineDependencyAnalyzeService.analysisResourceDependency(
                userId = userId,
                projectId = projectId,
                pipelineId = detail.pipelineId
            )
            copyTaskResourceRelSet.addAll(
                buildPipelineCopyResourceRelSet(
                    detail = detail,
                    dependentResources = dependentResources
                )
            )
            dependentResources
        }
        val pipelineReferCountMap = buildPipelineReferCountMap(copyTaskResourceRelSet)
        val copyTaskResourceSet = buildCopyResource(
            userId = userId,
            projectId = projectId,
            taskId = taskId,
            targetProjectId = targetProjectId,
            resources = resources.toSet(),
            pipelineReferCountMap = pipelineReferCountMap,
            pipelineCopyStrategy = pipelineCopyStrategy
        )

        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineCopyTaskResourceDao.batchCreate(
                dslContext = transactionContext,
                resources = copyTaskResourceSet.toList()
            )
            pipelineCopyTaskResourceRelDao.batchCreate(
                dslContext = transactionContext,
                relations = copyTaskResourceRelSet.toList()
            )
            pipelineBatchTaskDetailDao.updateStatus(
                dslContext = transactionContext,
                projectId = projectId,
                taskId = taskId,
                pipelineIds = pipelineIds,
                status = PipelineBatchTaskDetailStatus.WAIT_COPY,
                change = false
            )
        }
    }

    private fun buildCopyResource(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resources: Set<PipelineDependentResource>,
        pipelineReferCountMap: Map<String, Int>,
        pipelineCopyStrategy: PipelineCopyStrategy
    ): Set<PipelineCopyTaskResource> {
        val taskResources = mutableMapOf<String, PipelineCopyTaskResource>()
        resources.forEach { resource ->
            val copyResourceKey = resourceKey(resource.resourceType, resource.resourceId)
            if (taskResources.containsKey(copyResourceKey)) return@forEach
            val pipelineReferCount = pipelineReferCountMap[copyResourceKey] ?: 0
            when (resource.resourceType) {
                PipelineDependentResourceType.CREDENTIAL -> {
                    val credentialCopyResource = buildCredentialCopyResource(
                        userId = userId,
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = credentialCopyResource
                }

                PipelineDependentResourceType.REPOSITORY -> {
                    val repositoryCopyResource = buildRepositoryCopyResource(
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = repositoryCopyResource
                }

                PipelineDependentResourceType.BUILD_ENV, PipelineDependentResourceType.DEPLOY_ENV -> {
                    val envCopyTaskCopyResource = buildEnvCopyResource(
                        userId = userId,
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = envCopyTaskCopyResource
                }

                PipelineDependentResourceType.BUILD_NODE, PipelineDependentResourceType.DEPLOY_NODE -> {
                    val nodeCopyTaskCopyResource = buildNodeCopyResource(
                        userId = userId,
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = nodeCopyTaskCopyResource
                }

                PipelineDependentResourceType.PIPELINE_GROUP -> {
                    val pipelineGroupCopyResource = buildPipelineGroupCopyResource(
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = pipelineGroupCopyResource
                }

                PipelineDependentResourceType.PIPELINE_LABEL -> {
                    val pipelineLabelCopyResource = buildPipelineLabelCopyResource(
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = pipelineLabelCopyResource
                }

                PipelineDependentResourceType.PIPELINE_TEMPLATE -> {
                    val pipelineTemplateCopyResource = buildPipelineTemplateCopyResource(
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount
                    )
                    taskResources[copyResourceKey] = pipelineTemplateCopyResource
                }

                PipelineDependentResourceType.PIPELINE -> {
                    buildPipelineCopyResource(
                        projectId = projectId,
                        taskId = taskId,
                        targetProjectId = targetProjectId,
                        resource = resource,
                        pipelineReferCount = pipelineReferCount,
                        pipelineCopyStrategy = pipelineCopyStrategy
                    )?.let {
                        taskResources[copyResourceKey] = it
                    }
                }
            }
        }
        return taskResources.values.toSet()
    }

    private fun buildPipelineReferCountMap(
        relations: Set<PipelineCopyTaskResourceRel>
    ): Map<String, Int> {
        return relations.groupBy {
            resourceKey(it.resourceType, it.resourceId)
        }.mapValues { (_, resourceRelations) ->
            resourceRelations.map { it.pipelineId }.toSet().size
        }
    }

    private fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType}_$resourceId"
    }

    private fun buildCredentialCopyResource(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        try {
            client.get(ServiceCredentialResource::class).getBasicInfo(
                userId = userId,
                projectId = targetProjectId,
                credentialId = resource.resourceId
            ).data?.let {
                targetNameExists = true
            }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            targetProjectId = targetProjectId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildRepositoryCopyResource(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        var status = PipelineCopyTaskResourceStatus.UNPROCESSED
        var repoAuthCopyResourceProperties: RepoAuthCopyResourceProp? = null
        try {
            val repository = client.get(ServiceRepositoryResource::class).get(
                projectId = projectId,
                repositoryId = resource.resourceId,
                repositoryType = RepositoryType.ID
            ).data
            if (repository != null) {
                repoAuthCopyResourceProperties = buildRepositoryCopyResourceProperties(repository)
            } else {
                status = PipelineCopyTaskResourceStatus.FAILED
            }
        } catch (ignore: RemoteServiceException) {
            status = PipelineCopyTaskResourceStatus.FAILED
        }
        try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = targetProjectId,
                repositoryId = resource.resourceName,
                repositoryType = RepositoryType.NAME
            ).data?.let {
                targetNameExists = true
            }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            resourceProperties = repoAuthCopyResourceProperties,
            targetProjectId = targetProjectId,
            status = status,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildRepositoryCopyResourceProperties(repository: Repository): RepoAuthCopyResourceProp {
        val (authType, authInfo) = when (repository) {
            is CodeGitRepository -> repository.authType?.name to repository.getAuthInfo(repository.authType)
            is CodeTGitRepository -> repository.authType?.name to repository.getAuthInfo(repository.authType)
            is CodeGitlabRepository -> repository.authType?.name to repository.getAuthInfo(repository.authType)
            is ScmGitRepository -> repository.authType?.name to repository.getAuthInfo(repository.authType)
            is CodeSvnRepository -> repository.getSvnAuthType() to repository.credentialId
            is ScmSvnRepository -> repository.getSvnAuthType() to repository.credentialId
            is GithubRepository -> RepoAuthType.OAUTH.name to repository.userName
            is CodeP4Repository -> RepoAuthType.HTTP.name to repository.credentialId
            else -> null to null
        }
        return RepoAuthCopyResourceProp(
            authType = authType,
            authInfo = authInfo,
            repositoryType = repository.scmCode.ifBlank { repository.getScmType().name },
            repositoryUrl = repository.url
        )
    }

    private fun Repository.getAuthInfo(authType: RepoAuthType?): String {
        return if (authType == RepoAuthType.OAUTH) userName else credentialId
    }

    private fun CodeSvnRepository.getSvnAuthType(): String {
        return svnType?.uppercase() ?: RepoAuthType.SSH.name
    }

    private fun ScmSvnRepository.getSvnAuthType(): String {
        return svnType?.uppercase() ?: RepoAuthType.SSH.name
    }

    private fun buildEnvCopyResource(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        try {
            client.get(ServiceEnvironmentResource::class).getByName(
                userId = userId,
                projectId = targetProjectId,
                envName = resource.resourceName
            ).data?.let {
                targetNameExists = true
            }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            targetProjectId = targetProjectId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildNodeCopyResource(
        userId: String,
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        try {
            client.get(ServiceNodeResource::class).getNodeStatus(
                userId = userId,
                projectId = targetProjectId,
                nodeHashId = null,
                nodeName = resource.resourceName,
                agentHashId = null
            ).data?.let {
                targetNameExists = true
            }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            targetProjectId = targetProjectId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildPipelineLabelCopyResource(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var status = PipelineCopyTaskResourceStatus.UNPROCESSED
        var targetNameExists = false
        var labelGroupProp: PipelineLabelGroupCopyResourceProp? = null
        try {
            val sourceLabel = pipelineLabelDao.getById(
                dslContext = dslContext,
                projectId = projectId,
                id = HashUtil.decodeIdToLong(resource.resourceId)
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
                params = arrayOf(projectId, resource.resourceName)
            )
            val sourceGroup = pipelineGroupDao.get(
                dslContext = dslContext,
                projectId = projectId,
                groupId = sourceLabel.groupId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
                params = arrayOf(projectId, sourceLabel.groupId.toString())
            )
            labelGroupProp = PipelineLabelGroupCopyResourceProp(
                groupId = HashUtil.encodeLongId(sourceGroup.id),
                groupName = sourceGroup.name
            )
            pipelineGroupDao.getByName(
                dslContext = dslContext,
                projectId = targetProjectId,
                name = sourceGroup.name
            )?.let { targetGroup ->
                targetNameExists = pipelineLabelDao.getByName(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    groupId = targetGroup.id,
                    name = resource.resourceName
                ) != null
            } ?: run {
                targetNameExists = false
            }
        } catch (ignored: RemoteServiceException) {
            status = PipelineCopyTaskResourceStatus.FAILED
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            resourceProperties = labelGroupProp,
            targetProjectId = targetProjectId,
            status = status,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildPipelineGroupCopyResource(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        var resourceProperties: PipelineViewCopyResourceProp? = null
        try {
            pipelineViewDao.fetchAnyByName(
                dslContext = dslContext,
                projectId = targetProjectId,
                name = resource.resourceName,
                isProject = true
            )?.let {
                targetNameExists = true
                resourceProperties = PipelineViewCopyResourceProp(
                    viewType = it.viewType
                )
            }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            resourceProperties = resourceProperties,
            targetProjectId = targetProjectId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildPipelineTemplateCopyResource(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int
    ): PipelineCopyTaskResource {
        var targetNameExists = false
        try {
            pipelineTemplateInfoDao.isNameExist(
                dslContext = dslContext,
                projectId = targetProjectId,
                templateName = resource.resourceName,
                excludeTemplateId = null
            ).let { targetNameExists = it }
        } catch (ignore: RemoteServiceException) {
            targetNameExists = false
        }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            targetProjectId = targetProjectId,
            status = PipelineCopyTaskResourceStatus.UNPROCESSED,
            targetNameExists = targetNameExists,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildPipelineCopyResource(
        projectId: String,
        taskId: String,
        targetProjectId: String,
        resource: PipelineDependentResource,
        pipelineReferCount: Int,
        pipelineCopyStrategy: PipelineCopyStrategy
    ): PipelineCopyTaskResource? {
        val targetPipelineId = when (pipelineCopyStrategy) {
            PipelineCopyStrategy.PIPELINE_CREATE_NEW_ID -> {
                pipelineIdGenerator.getNextId()
            }

            PipelineCopyStrategy.PIPELINE_REUSE_SOURCE_ID -> {
                resource.resourceId
            }

            else -> {
                logger.info(
                    "unknown pipeline copy strategy|$projectId|$taskId|${resource.resourceId}|$pipelineCopyStrategy"
                )
                return null
            }
        }
        val targetIdPipelineInfo = pipelineInfoDao.getPipelineId(
            dslContext = dslContext,
            projectId = targetProjectId,
            pipelineId = targetPipelineId
        )
        val targetNamePipelineInfo = pipelineInfoDao.getPipelineInfoByName(
            dslContext = dslContext,
            projectId = targetProjectId,
            pipelineName = resource.resourceName
        )
        val (pipelineConflictCopyResourceProp, status) =
            if (targetIdPipelineInfo != null || targetNamePipelineInfo != null) {
                val pipelineConflictCopyResourceProp = PipelineConflictCopyResourceProp(
                    idConflict = targetIdPipelineInfo?.let {
                        PipelineConflictInfo(
                            pipelineId = it.pipelineId,
                            pipelineName = it.pipelineName,
                            creator = it.creator
                        )
                    },
                    nameConflict = targetNamePipelineInfo?.let {
                        PipelineConflictInfo(
                            pipelineId = it.pipelineId,
                            pipelineName = it.pipelineName,
                            creator = it.creator
                        )
                    }
                )
                Pair(pipelineConflictCopyResourceProp, PipelineCopyTaskResourceStatus.UNPROCESSED)
            } else {
                Pair(null, PipelineCopyTaskResourceStatus.PROCESSED)
            }
        return PipelineCopyTaskResource(
            taskId = taskId,
            projectId = projectId,
            resourceType = resource.resourceType,
            resourceId = resource.resourceId,
            resourceName = resource.resourceName,
            resourceProperties = pipelineConflictCopyResourceProp,
            targetProjectId = targetProjectId,
            status = status,
            targetNameExists = targetNamePipelineInfo != null,
            targetIdExists = targetIdPipelineInfo != null,
            pipelineReferCount = pipelineReferCount
        )
    }

    private fun buildPipelineCopyResourceRelSet(
        detail: PipelineBatchTaskDetail,
        dependentResources: Set<PipelineDependentResource>
    ): Set<PipelineCopyTaskResourceRel> {
        return dependentResources.map { resource ->
            PipelineCopyTaskResourceRel(
                taskId = detail.taskId,
                projectId = detail.projectId,
                pipelineId = detail.pipelineId,
                pipelineName = detail.pipelineName,
                resourceType = resource.resourceType,
                resourceId = resource.resourceId,
                resourceName = resource.resourceName
            )
        }.toSet()
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
            val changeCount = pipelineBatchTaskDetailDao.count(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                change = true
            )
            if (changeCount == 0L) {
                logger.warn("pipeline batch task has |$projectId|$taskId")
                return null
            }
            pipelineBatchTaskDao.updateStatus(
                dslContext = dslContext,
                projectId = projectId,
                taskId = taskId,
                status = PipelineBatchTaskStatus.PIPELINE_RESOURCE_ANALYZING
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
        private val logger = LoggerFactory.getLogger(PipelineCopyTaskAnalyzeService::class.java)
    }
}
