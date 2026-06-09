package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.enums.EnvType
import com.tencent.devops.environment.pojo.enums.NodeSource
import com.tencent.devops.process.constant.PipelineViewType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.utils.CredentialUtils
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
import com.tencent.devops.ticket.pojo.CredentialCreate
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

/**
 * 流水线复制资源创建服务
 */
@Service
class PipelineCopyResourceCreateService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewService: PipelineViewService,
    private val pipelineDependencyReplaceService: PipelineDependencyReplaceService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService
) {
    fun createCredential(
        userId: String,
        sourceProjectId: String,
        credentialId: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val pair = DHUtil.initKey()
        val encoder = Base64.getEncoder()
        val credentialBasicInfo = client.get(ServiceCredentialResource::class).getBasicInfo(
            userId = userId,
            projectId = sourceProjectId,
            credentialId = credentialId
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, credentialId)
        )
        val credentialResult = client.get(ServiceCredentialResource::class).get(
            projectId = sourceProjectId,
            credentialId = credentialId,
            publicKey = encoder.encodeToString(pair.publicKey),
            padding = true
        )
        if (credentialResult.isNotOk() || credentialResult.data == null) {
            throw ErrorCodeException(
                errorCode = credentialResult.status.toString(),
                defaultMessage = credentialResult.message
            )
        }

        val credential = credentialResult.data!!
        // 凭证字段定义: com.tencent.devops.ticket.pojo.enums.CredentialType
        val v1 = CredentialUtils.decode(
            encode = credential.v1,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val v2 = CredentialUtils.decode(
            encode = credential.v2,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val v3 = CredentialUtils.decode(
            encode = credential.v3,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val v4 = CredentialUtils.decode(
            encode = credential.v3,
            publicKey = credential.publicKey,
            privateKey = pair.privateKey
        )
        val credentialCreate = CredentialCreate(
            credentialId = credentialBasicInfo.credentialId,
            credentialName = credentialBasicInfo.credentialName,
            credentialType = credentialBasicInfo.credentialType,
            v1 = v1,
            v2 = v2,
            v3 = v3,
            v4 = v4
        )
        val createResult = client.get(ServiceCredentialResource::class).create(
            userId = userId,
            projectId = targetProjectId,
            credential = credentialCreate
        )
        checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = credentialId,
            result = createResult
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = credentialId,
            resourceName = credentialId
        )
    }

    fun createRepository(
        userId: String,
        sourceProjectId: String,
        repoName: String,
        targetProjectId: String,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): PipelineCopyResourceBasicInfo {
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = sourceProjectId,
            repositoryId = repoName,
            repositoryType = RepositoryType.NAME
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, repoName)
        )
        val targetRepository = buildTargetRepository(
            userId = userId,
            repository = repository,
            resourceMap = resourceMap
        )
        val createResult = client.get(ServiceRepositoryResource::class).create(
            userId = userId,
            projectId = targetProjectId,
            repository = targetRepository
        )
        val targetRepoHashId = checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = repoName,
            result = createResult
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = targetRepoHashId.hashId,
            resourceName = repoName
        )
    }

    fun moveNodeToTargetProject(
        userId: String,
        sourceProjectId: String,
        nodeHashId: String?,
        agentHashId: String?,
        targetProjectId: String,
        resourceType: PipelineDependentResourceType
    ): PipelineCopyResourceBasicInfo {
        val sourceNode = client.get(ServiceNodeResource::class).getNodeStatus(
            userId = userId,
            projectId = sourceProjectId,
            nodeHashId = nodeHashId,
            nodeName = null,
            agentHashId = agentHashId
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, nodeHashId ?: agentHashId ?: "")
        )
        val transferResult = client.get(ServiceNodeResource::class).transferNode(
            userId = userId,
            projectId = sourceProjectId,
            targetProjectId = targetProjectId,
            nodeHashId = nodeHashId,
            agentHashId = agentHashId
        )
        checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceNode.displayName ?: sourceNode.name,
            result = transferResult
        )
        return PipelineCopyResourceBasicInfo(
            // 构建节点,流水线使用的agentHashId
            resourceId = if (resourceType == PipelineDependentResourceType.BUILD_NODE) {
                sourceNode.agentHashId!!
            } else {
                sourceNode.nodeHashId
            },
            resourceName = sourceNode.displayName ?: sourceNode.name
        )
    }

    fun createPipelineGroup(
        userId: String,
        sourceProjectId: String,
        viewName: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val sourceView = pipelineViewDao.fetchAnyByName(
            dslContext = dslContext,
            projectId = sourceProjectId,
            name = viewName,
            isProject = true
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, viewName)
        )

        val targetViewId = pipelineViewService.addView(
            userId = userId,
            projectId = targetProjectId,
            pipelineView = PipelineViewForm(
                name = sourceView.name,
                projected = true,
                viewType = sourceView.viewType,
                logic = Logic.of(sourceView.logic),
                filters = pipelineViewService.getFilters(
                    filterByName = sourceView.filterByPipeineName,
                    filterByCreator = sourceView.filterByCreator,
                    filters = sourceView.filters
                ),
                pipelineIds = if (sourceView.viewType == PipelineViewType.STATIC) emptyList() else null
            )
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = HashUtil.encodeLongId(targetViewId),
            resourceName = sourceView.name
        )
    }

    fun createPipelineLabel(
        userId: String,
        sourceProjectId: String,
        sourceLabelId: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val sourceLabel = pipelineLabelDao.getById(
            dslContext = dslContext,
            projectId = sourceProjectId,
            id = HashUtil.decodeIdToLong(sourceLabelId)
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, sourceLabelId)
        )
        val sourceGroup = pipelineGroupDao.get(
            dslContext = dslContext,
            projectId = sourceProjectId,
            groupId = sourceLabel.groupId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, sourceLabel.groupId.toString())
        )
        var targetGroup = pipelineGroupDao.getByName(
            dslContext = dslContext,
            projectId = targetProjectId,
            name = sourceGroup.name
        )
        if (targetGroup == null) {
            pipelineGroupService.addGroup(
                userId = userId,
                pipelineGroup = PipelineGroupCreate(
                    projectId = targetProjectId,
                    name = sourceGroup.name
                )
            )
            targetGroup = pipelineGroupDao.getByName(
                dslContext = dslContext,
                projectId = targetProjectId,
                name = sourceGroup.name
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_CREATE_FAILED,
                params = arrayOf(targetProjectId, sourceGroup.name)
            )
        }
        pipelineGroupService.addLabel(
            userId = userId,
            projectId = targetProjectId,
            pipelineLabel = PipelineLabelCreate(
                groupId = HashUtil.encodeLongId(targetGroup.id),
                name = sourceLabel.name
            )
        )
        val targetLabel = pipelineLabelDao.getByName(
            dslContext = dslContext,
            projectId = targetProjectId,
            groupId = targetGroup.id,
            name = sourceLabel.name
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_CREATE_FAILED,
            params = arrayOf(targetProjectId, sourceLabel.name)
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = HashUtil.encodeLongId(targetLabel.id),
            resourceName = targetLabel.name
        )
    }

    fun createEnv(
        userId: String,
        sourceProjectId: String,
        sourceEnvHashId: String,
        targetProjectId: String,
        nodeHashIds: List<String>
    ): PipelineCopyResourceBasicInfo {
        val sourceEnv = client.get(ServiceEnvironmentResource::class).get(
            userId = userId,
            projectId = sourceProjectId,
            envHashId = sourceEnvHashId,
            checkPermission = false
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, sourceEnvHashId)
        )
        val createResult = client.get(ServiceEnvironmentResource::class).create(
            userId = userId,
            projectId = targetProjectId,
            environment = EnvCreateInfo(
                name = sourceEnv.name,
                desc = sourceEnv.desc,
                envType = EnvType.valueOf(sourceEnv.envType),
                envVars = sourceEnv.envVars,
                source = NodeSource.EXISTING,
                nodeHashIds = nodeHashIds
            )
        )
        val envId = checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceEnv.name,
            result = createResult
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = envId.hashId,
            resourceName = sourceEnv.name
        )
    }

    fun createEnvAndMoveNodes(
        userId: String,
        sourceProjectId: String,
        sourceEnvHashId: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val sourceEnv = client.get(ServiceEnvironmentResource::class).get(
            userId = userId,
            projectId = sourceProjectId,
            envHashId = sourceEnvHashId,
            checkPermission = false
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, sourceEnvHashId)
        )
        val envId = checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceEnv.name,
            result = client.get(ServiceEnvironmentResource::class).createEnvAndTransferNodes(
                userId = userId,
                projectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourceEnvHashId = sourceEnvHashId
            )
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = envId.hashId,
            resourceName = sourceEnv.name
        )
    }

    fun createPipeline(
        userId: String,
        sourceProjectId: String,
        sourcePipelineId: String,
        targetProjectId: String,
        targetPipelineId: String,
        targetPipelineName: String,
        dependentResources: List<PipelineCopyTaskResource>
    ): PipelineCopyResourceBasicInfo {
        val modelAndSetting = pipelineDependencyReplaceService.replaceResourceDependency(
            userId = userId,
            projectId = sourceProjectId,
            pipelineId = sourcePipelineId,
            targetProjectId = targetProjectId,
            targetPipelineId = targetPipelineId,
            targetPipelineName = targetPipelineName,
            replaceResourceMap = buildReplaceResourceMap(dependentResources)
        )
        pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = targetProjectId,
            model = modelAndSetting.model,
            channelCode = ChannelCode.BS,
            setting = modelAndSetting.setting,
            fixPipelineId = targetPipelineId
        )
        associateStaticPipelineGroups(
            userId = userId,
            targetProjectId = targetProjectId,
            targetPipelineId = targetPipelineId,
            dependentResources = dependentResources
        )

        return PipelineCopyResourceBasicInfo(
            resourceId = targetPipelineId,
            resourceName = targetPipelineName
        )
    }

    private fun associateStaticPipelineGroups(
        userId: String,
        targetProjectId: String,
        targetPipelineId: String,
        dependentResources: List<PipelineCopyTaskResource>
    ) {
        dependentResources.filter {
            it.resourceType == PipelineDependentResourceType.PIPELINE_GROUP &&
                it.status == PipelineCopyTaskResourceStatus.SUCCESS
        }.forEach { resource ->
            val targetViewId = parseViewId(resource.targetResourceId) ?: return@forEach
            val targetView = pipelineViewDao.get(
                dslContext = dslContext,
                projectId = targetProjectId,
                viewId = targetViewId
            ) ?: return@forEach
            if (targetView.isProject && targetView.viewType == PipelineViewType.STATIC) {
                pipelineViewGroupDao.create(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    viewId = targetViewId,
                    pipelineId = targetPipelineId,
                    userId = userId
                )
            }
        }
    }

    private fun parseViewId(viewId: String?): Long? {
        if (viewId.isNullOrBlank()) {
            return null
        }
        return HashUtil.decodeIdToLong(viewId)
    }

    private fun buildTargetRepository(
        userId: String,
        repository: Repository,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): Repository {
        val (authType, sourceCredentialId) = repository.getRepoAuthInfo()
        if (authType.isNullOrBlank()) {
            return repository
        }
        return if (authType == RepoAuthType.OAUTH.name) {
            repository.copyUserName(userId = userId)
        } else {
            val targetCredentialId = getTargetCredentialId(
                sourceCredentialId = sourceCredentialId,
                resourceMap = resourceMap
            )
            repository.copyCredentialId(credentialId = targetCredentialId)
        }
    }

    private fun Repository.getRepoAuthInfo(): Pair<String?, String?> {
        return when (this) {
            is CodeGitRepository -> (authType ?: RepoAuthType.SSH).name to credentialId
            is CodeTGitRepository -> (authType ?: RepoAuthType.SSH).name to credentialId
            is CodeGitlabRepository -> (authType ?: RepoAuthType.HTTP).name to credentialId
            is ScmGitRepository -> (authType ?: RepoAuthType.SSH).name to credentialId
            is CodeSvnRepository -> getSvnAuthType() to credentialId
            is ScmSvnRepository -> getSvnAuthType() to credentialId
            is GithubRepository -> RepoAuthType.OAUTH.name to credentialId
            is CodeP4Repository -> RepoAuthType.HTTP.name to credentialId
            else -> null to null
        }
    }

    private fun getTargetCredentialId(
        sourceCredentialId: String?,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): String {
        val sourceCredential = sourceCredentialId?.takeIf { it.isNotBlank() } ?: throwDependencyFailed(
            resourceType = PipelineDependentResourceType.CREDENTIAL,
            resourceName = sourceCredentialId.orEmpty()
        )
        val credentialResource = resourceMap[
            PipelineCopyTaskUtils.resourceKey(PipelineDependentResourceType.CREDENTIAL, sourceCredential)
        ]
            ?: throwDependencyFailed(PipelineDependentResourceType.CREDENTIAL, sourceCredential)
        return credentialResource.targetResourceId?.takeIf { it.isNotBlank() }
            ?: throwDependencyFailed(PipelineDependentResourceType.CREDENTIAL, sourceCredential)
    }

    private fun Repository.copyUserName(userId: String): Repository {
        return when (this) {
            is CodeGitRepository -> copy(userName = userId)
            is CodeTGitRepository -> copy(userName = userId)
            is CodeGitlabRepository -> copy(userName = userId)
            is ScmGitRepository -> copy(userName = userId)
            is CodeSvnRepository -> copy(userName = userId)
            is ScmSvnRepository -> copy(userName = userId)
            is GithubRepository -> copy(userName = userId)
            is CodeP4Repository -> copy(userName = userId)
            else -> this
        }
    }

    private fun Repository.copyCredentialId(credentialId: String): Repository {
        return when (this) {
            is CodeGitRepository -> copy(credentialId = credentialId)
            is CodeTGitRepository -> copy(credentialId = credentialId)
            is CodeGitlabRepository -> copy(credentialId = credentialId)
            is ScmGitRepository -> copy(credentialId = credentialId)
            is CodeSvnRepository -> copy(credentialId = credentialId)
            is ScmSvnRepository -> copy(credentialId = credentialId)
            is GithubRepository -> copy(credentialId = credentialId)
            is CodeP4Repository -> copy(credentialId = credentialId)
            else -> this
        }
    }

    private fun CodeSvnRepository.getSvnAuthType(): String {
        return svnType?.uppercase() ?: RepoAuthType.SSH.name
    }

    private fun ScmSvnRepository.getSvnAuthType(): String {
        return svnType?.uppercase() ?: RepoAuthType.SSH.name
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

    private fun <T> checkTargetResourceCreateResult(
        targetProjectId: String,
        resourceName: String,
        result: Result<T>
    ): T {
        if (result.isNotOk() || result.data == null) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_TARGET_RESOURCE_CREATE_FAILED,
                params = arrayOf(targetProjectId, resourceName, result.message.orEmpty())
            )
        }
        return result.data!!
    }

    private fun buildReplaceResourceMap(
        resources: List<PipelineCopyTaskResource>
    ): Map<String, PipelineDependentResource> {
        return resources.mapNotNull { resource ->
            val targetProjectId = resource.targetProjectId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val targetResourceId = resource.targetResourceId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val targetResourceName = resource.targetResourceName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val key = PipelineCopyTaskUtils.resourceKey(resource.resourceType, resource.resourceId)
            key to PipelineDependentResource(
                projectId = targetProjectId,
                resourceType = resource.resourceType,
                resourceId = targetResourceId,
                resourceName = targetResourceName
            )
        }.toMap()
    }
}
