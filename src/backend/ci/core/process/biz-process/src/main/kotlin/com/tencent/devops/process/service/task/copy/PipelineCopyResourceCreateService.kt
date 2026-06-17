package com.tencent.devops.process.service.task.copy

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
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
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.pipeline.task.PipelineCopyTaskResource
import com.tencent.devops.process.pojo.pipeline.task.PipelineTemplateCopyResourceProp
import com.tencent.devops.process.service.PipelineInfoFacadeService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
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
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import com.tencent.devops.ticket.pojo.CredentialCreate
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Base64

/**
 * 流水线复制资源创建服务
 */
@Service
class PipelineCopyResourceCreateService @Autowired constructor(
    private val client: Client,
    private val clientTokenService: ClientTokenService,
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewService: PipelineViewService,
    private val pipelineDependencyReplaceService: PipelineDependencyReplaceService,
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService
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
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.TICKET_CREDENTIAL.value,
            sourceResourceCode = credentialId,
            targetResourceCode = credentialId
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
        // oauth代码库,还是应该使用原有的oauth授权身份
        val (authType, targetRepository) = buildTargetRepository(
            repository = repository,
            resourceMap = resourceMap
        )
        val createUserId = if (authType == RepoAuthType.OAUTH) {
            repository.userName
        } else {
            userId
        }
        val createResult = client.get(ServiceRepositoryResource::class).create(
            userId = createUserId,
            projectId = targetProjectId,
            repository = targetRepository
        )
        val targetRepoHashId = checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = repoName,
            result = createResult
        )
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.CODE_REPERTORY.value,
            sourceResourceCode = repository.repoHashId!!,
            targetResourceCode = targetRepoHashId.hashId
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = targetRepoHashId.hashId,
            resourceName = repoName
        )
    }

    fun moveBuildNodeToTargetProject(
        userId: String,
        sourceProjectId: String,
        agentHashId: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val sourceNode = client.get(ServiceNodeResource::class).getNodeStatus(
            userId = userId,
            projectId = sourceProjectId,
            nodeHashId = null,
            nodeName = null,
            agentHashId = agentHashId
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, agentHashId)
        )
        val transferResult = client.get(ServiceNodeResource::class).transferNode(
            userId = userId,
            projectId = sourceProjectId,
            targetProjectId = targetProjectId,
            nodeHashId = null,
            agentHashId = agentHashId
        )
        checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceNode.displayName ?: sourceNode.name,
            result = transferResult
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = sourceNode.agentHashId!!,
            resourceName = sourceNode.displayName ?: sourceNode.name
        )
    }

    fun moveDeployNodeToTargetProject(
        userId: String,
        sourceProjectId: String,
        nodeHashId: String,
        targetProjectId: String
    ): PipelineCopyResourceBasicInfo {
        val sourceNode = client.get(ServiceNodeResource::class).getRawNode(
            userId = userId,
            projectId = sourceProjectId,
            nodeHashId = nodeHashId,
            nodeName = null
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, nodeHashId)
        )
        val transferResult = client.get(ServiceNodeResource::class).transferNode(
            userId = userId,
            projectId = sourceProjectId,
            targetProjectId = targetProjectId,
            nodeHashId = nodeHashId,
            agentHashId = null
        )
        checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceNode.displayName ?: sourceNode.name,
            result = transferResult
        )
        return PipelineCopyResourceBasicInfo(
            // 构建节点,流水线使用的agentHashId
            resourceId = sourceNode.nodeHashId,
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
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.PIPELINE_GROUP.value,
            sourceResourceCode = HashUtil.encodeLongId(sourceView.id),
            targetResourceCode = HashUtil.encodeLongId(targetViewId)
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
                nodeHashIds = nodeHashIds,
                nodeTags = null
            )
        )
        val envId = checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceEnv.name,
            result = createResult
        )
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
            sourceResourceCode = sourceEnvHashId,
            targetResourceCode = envId.hashId
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
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
            sourceResourceCode = sourceEnvHashId,
            targetResourceCode = envId.hashId
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = envId.hashId,
            resourceName = sourceEnv.name
        )
    }

    fun createEnvAndRelateSameNameNodes(
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
            result = client.get(ServiceEnvironmentResource::class).createEnvAndRelateSameNameNodes(
                userId = userId,
                projectId = sourceProjectId,
                targetProjectId = targetProjectId,
                sourceEnvHashId = sourceEnvHashId
            )
        )
        copyResourceGroupMembersSafely(
            sourceProjectId = sourceProjectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.ENVIRONMENT_ENVIRONMENT.value,
            sourceResourceCode = sourceEnvHashId,
            targetResourceCode = envId.hashId
        )
        return PipelineCopyResourceBasicInfo(
            resourceId = envId.hashId,
            resourceName = sourceEnv.name
        )
    }

    fun createPipeline(
        userId: String,
        projectId: String,
        sourcePipelineId: String,
        targetProjectId: String,
        targetPipelineId: String,
        targetPipelineName: String,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): PipelineCopyResourceBasicInfo {
        val templateVersion = resolveTemplateVersion(
            projectId = projectId,
            sourcePipelineId = sourcePipelineId,
            resourceMap = resourceMap
        )
        val modelAndSetting = pipelineDependencyReplaceService.replacePipelineResourceDependency(
            userId = userId,
            projectId = projectId,
            pipelineId = sourcePipelineId,
            targetProjectId = targetProjectId,
            targetPipelineId = targetPipelineId,
            targetPipelineName = targetPipelineName,
            resourceMap = resourceMap
        )
        // 校验插件是否在目标项目可见
        client.get(ServiceTemplateResource::class).validateModelComponentVisibleDept(
            userId = userId,
            model = modelAndSetting.model,
            projectCode = targetProjectId
        )
        pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = targetProjectId,
            model = modelAndSetting.model,
            channelCode = ChannelCode.BS,
            setting = modelAndSetting.setting,
            instanceType = if (modelAndSetting.model.instanceFromTemplate == true) {
                PipelineInstanceTypeEnum.CONSTRAINT.type
            } else {
                PipelineInstanceTypeEnum.FREEDOM.type
            },
            fixPipelineId = targetPipelineId,
            fixTemplateVersion = templateVersion
        )
        copyResourceGroupMembersSafely(
            sourceProjectId = projectId,
            targetProjectId = targetProjectId,
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            sourceResourceCode = sourcePipelineId,
            targetResourceCode = targetPipelineId
        )

        return PipelineCopyResourceBasicInfo(
            resourceId = targetPipelineId,
            resourceName = targetPipelineName
        )
    }

    private fun resolveTemplateVersion(
        projectId: String,
        sourcePipelineId: String,
        resourceMap: Map<String, PipelineCopyTaskResource>
    ): Long? {
        val related = pipelineTemplateRelatedService.get(
            projectId = projectId,
            pipelineId = sourcePipelineId
        ) ?: return null
        if (related.instanceType != PipelineInstanceTypeEnum.CONSTRAINT) {
            return null
        }
        val templateResource = resourceMap[
            PipelineCopyTaskUtils.resourceKey(
                resourceType = PipelineDependentResourceType.PIPELINE_TEMPLATE,
                resourceId = related.templateId
            )
        ] ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_NOT_EXISTS,
            params = arrayOf(projectId, "${related.templateId}@${related.versionName}")
        )
        if (templateResource.status != PipelineCopyTaskResourceStatus.SUCCESS) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_FAILED,
                params = arrayOf("PIPELINE_TEMPLATE:${templateResource.resourceName}")
            )
        }
        val versionMappings = (templateResource.targetResourceProp as? PipelineTemplateCopyResourceProp)
            ?.versionMappings
        val versionMapping = versionMappings?.find { it.sourceVersion == related.version }
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_DEPENDENT_RESOURCE_FAILED,
                params = arrayOf("PIPELINE_TEMPLATE:${related.templateId}@${related.versionName}")
            )
        return versionMapping.targetVersion
    }

    private fun buildTargetRepository(
        repository: Repository,
        resourceMap: MutableMap<String, PipelineCopyTaskResource>
    ): Pair<RepoAuthType, Repository> {
        val (authType, sourceCredentialId) = repository.getRepoAuthInfo()
        return if (authType == RepoAuthType.OAUTH.name) {
            Pair(RepoAuthType.OAUTH, repository)
        } else {
            val targetCredentialId = getTargetCredentialId(
                sourceCredentialId = sourceCredentialId,
                resourceMap = resourceMap
            )
            Pair(RepoAuthType.OAUTH, repository.copyCredentialId(credentialId = targetCredentialId))
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

    private fun Repository.copyCredentialId(credentialId: String): Repository {
        return when (this) {
            is CodeGitRepository -> copy(credentialId = credentialId, enablePac = false)
            is CodeTGitRepository -> copy(credentialId = credentialId, enablePac = false)
            is CodeGitlabRepository -> copy(credentialId = credentialId, enablePac = false)
            is ScmGitRepository -> copy(credentialId = credentialId, enablePac = false)
            is CodeSvnRepository -> copy(credentialId = credentialId, enablePac = false)
            is ScmSvnRepository -> copy(credentialId = credentialId, enablePac = false)
            is GithubRepository -> copy(credentialId = credentialId, enablePac = false)
            is CodeP4Repository -> copy(credentialId = credentialId, enablePac = false)
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

    private fun copyResourceGroupMembersSafely(
        sourceProjectId: String,
        targetProjectId: String,
        resourceType: String,
        sourceResourceCode: String,
        targetResourceCode: String
    ) {
        try {
            client.get(ServiceResourceMemberResource::class).copyResourceGroupMembers(
                token = clientTokenService.getSystemToken() ?: "",
                sourceProjectCode = sourceProjectId,
                resourceType = resourceType,
                sourceResourceCode = sourceResourceCode,
                targetProjectCode = targetProjectId,
                targetResourceCode = targetResourceCode
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy resource group members failed|$sourceProjectId|$targetProjectId|" +
                    "$resourceType|$sourceResourceCode|$targetResourceCode",
                ignored
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineCopyResourceCreateService::class.java)
    }
}
