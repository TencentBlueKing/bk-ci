package com.tencent.devops.process.service.task

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
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
import com.tencent.devops.process.pojo.classify.PipelineViewForm
import com.tencent.devops.process.pojo.classify.enums.Logic
import com.tencent.devops.process.pojo.pipeline.task.RepoAuthCopyResourceProp
import com.tencent.devops.process.service.view.PipelineViewService
import com.tencent.devops.process.utils.CredentialUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
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
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewService: PipelineViewService
) {
    fun createCredential(
        userId: String,
        sourceProjectId: String,
        credentialId: String,
        targetProjectId: String
    ): PipelineCopyTargetResource {
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
        return PipelineCopyTargetResource(
            resourceId = credentialId,
            resourceName = credentialId
        )
    }

    fun createRepository(
        userId: String,
        sourceProjectId: String,
        repoName: String,
        targetProjectId: String,
        targetRepoAuthCopyResourceProp: RepoAuthCopyResourceProp
    ): PipelineCopyTargetResource {
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
            targetRepoAuthCopyResourceProp = targetRepoAuthCopyResourceProp
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
        return PipelineCopyTargetResource(
            resourceId = targetRepoHashId.hashId,
            resourceName = repoName
        )
    }

    fun moveNodeToTargetProject(
        userId: String,
        sourceProjectId: String,
        nodeHashId: String,
        targetProjectId: String
    ): PipelineCopyTargetResource {
        val sourceNode = client.get(ServiceNodeResource::class).getNodeStatus(
            userId = userId,
            projectId = sourceProjectId,
            nodeHashId = nodeHashId,
            nodeName = null,
            agentHashId = null
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, nodeHashId)
        )
        val transferResult = client.get(ServiceNodeResource::class).transferNodes(
            userId = userId,
            projectId = sourceProjectId,
            targetProjectId = targetProjectId,
            nodeHashIds = listOf(nodeHashId)
        )
        checkTargetResourceCreateResult(
            targetProjectId = targetProjectId,
            resourceName = sourceNode.displayName ?: sourceNode.name,
            result = transferResult
        )
        return PipelineCopyTargetResource(
            resourceId = sourceNode.nodeHashId,
            resourceName = sourceNode.displayName ?: sourceNode.name
        )
    }

    fun createPipelineGroup(
        userId: String,
        sourceProjectId: String,
        viewName: String,
        targetProjectId: String
    ): PipelineCopyTargetResource {
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
        return PipelineCopyTargetResource(
            resourceId = HashUtil.encodeLongId(targetViewId),
            resourceName = sourceView.name
        )
    }

    fun createPipelineLabel(
        userId: String,
        sourceProjectId: String,
        sourceLabelId: String,
        targetProjectId: String
    ): PipelineCopyTargetResource {
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
        val targetGroupId = pipelineGroupDao.list(
            dslContext = dslContext,
            projectId = targetProjectId
        ).firstOrNull { it.name == sourceGroup.name }?.id ?: pipelineGroupDao.create(
            dslContext = dslContext,
            projectId = targetProjectId,
            name = sourceGroup.name,
            userId = userId,
            id = generateSegmentId("PIPELINE_GROUP")
        )
        val targetLabelId = generateSegmentId("PIPELINE_LABEL")
        pipelineLabelDao.create(
            dslContext = dslContext,
            projectId = targetProjectId,
            groupId = targetGroupId,
            name = sourceLabel.name,
            userId = userId,
            id = targetLabelId
        )
        return PipelineCopyTargetResource(
            resourceId = HashUtil.encodeLongId(targetLabelId),
            resourceName = sourceLabel.name
        )
    }

    fun createEnv(
        userId: String,
        sourceProjectId: String,
        sourceEnvHashId: String,
        targetProjectId: String,
        nodeHashIds: List<String>
    ): PipelineCopyTargetResource {
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
        return PipelineCopyTargetResource(
            resourceId = envId.hashId,
            resourceName = sourceEnv.name
        )
    }

    fun createEnvAndMoveNodes(
        userId: String,
        sourceProjectId: String,
        sourceEnvHashId: String,
        targetProjectId: String
    ): PipelineCopyTargetResource {
        val sourceNodes = client.get(ServiceEnvironmentResource::class).listNodesByEnvIds(
            userId = userId,
            projectId = sourceProjectId,
            envHashIds = listOf(sourceEnvHashId)
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
            params = arrayOf(sourceProjectId, sourceEnvHashId)
        )
        val sourceNodeHashIds = sourceNodes.map { it.nodeHashId }
        if (sourceNodeHashIds.isNotEmpty()) {
            client.get(ServiceNodeResource::class).transferNodes(
                userId = userId,
                projectId = sourceProjectId,
                targetProjectId = targetProjectId,
                nodeHashIds = sourceNodeHashIds
            )
        }
        return createEnv(
            userId = userId,
            sourceProjectId = sourceProjectId,
            sourceEnvHashId = sourceEnvHashId,
            targetProjectId = targetProjectId,
            nodeHashIds = sourceNodeHashIds
        )
    }

    private fun buildTargetRepository(
        userId: String,
        repository: Repository,
        targetRepoAuthCopyResourceProp: RepoAuthCopyResourceProp
    ): Repository {
        return if (targetRepoAuthCopyResourceProp.authType == RepoAuthType.OAUTH.name) {
            when (repository) {
                is CodeGitRepository -> repository.copy(userName = userId)
                is CodeTGitRepository -> repository.copy(userName = userId)
                is CodeGitlabRepository -> repository.copy(userName = userId)
                is ScmGitRepository -> repository.copy(userName = userId)
                is CodeSvnRepository -> repository.copy(userName = userId)
                is ScmSvnRepository -> repository.copy(userName = userId)
                is GithubRepository -> repository.copy(userName = userId)
                is CodeP4Repository -> repository.copy(userName = userId)
                else -> repository
            }
        } else {
            val credentialId = targetRepoAuthCopyResourceProp.authInfo.orEmpty()
            when (repository) {
                is CodeGitRepository -> repository.copy(credentialId = credentialId)
                is CodeTGitRepository -> repository.copy(credentialId = credentialId)
                is CodeGitlabRepository -> repository.copy(credentialId = credentialId)
                is ScmGitRepository -> repository.copy(credentialId = credentialId)
                is CodeSvnRepository -> repository.copy(credentialId = credentialId)
                is ScmSvnRepository -> repository.copy(credentialId = credentialId)
                is GithubRepository -> repository.copy(credentialId = credentialId)
                is CodeP4Repository -> repository.copy(credentialId = credentialId)
                else -> repository
            }
        }
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

    private fun generateSegmentId(bizTag: String): Long {
        val result = client.get(ServiceAllocIdResource::class).generateSegmentId(bizTag)
        return result.data ?: throw ErrorCodeException(
            errorCode = result.status.toString(),
            defaultMessage = result.message
        )
    }
}
