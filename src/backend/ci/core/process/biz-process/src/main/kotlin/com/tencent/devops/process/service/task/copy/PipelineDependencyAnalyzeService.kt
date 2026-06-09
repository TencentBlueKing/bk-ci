package com.tencent.devops.process.service.task.copy

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.SubPipelineRefService
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResourceRef
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceRefType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.CodeGitlabRepository
import com.tencent.devops.repository.pojo.CodeP4Repository
import com.tencent.devops.repository.pojo.CodeSvnRepository
import com.tencent.devops.repository.pojo.CodeTGitRepository
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.ScmGitRepository
import com.tencent.devops.repository.pojo.ScmSvnRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
/**
 * 流水线依赖资源分析
 */
class PipelineDependencyAnalyzeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val subPipelineRefService: SubPipelineRefService,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    fun analysisResourceDependency(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        val resources = mutableSetOf<PipelineDependentResource>()
        val pipelineInfo = pipelineInfoDao.getPipelineInfo(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        resources.add(
            PipelineDependentResource(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.PIPELINE,
                resourceId = pipelineId,
                resourceName = pipelineInfo.pipelineName
            )
        )
        resources.addAll(collectPipelineGroupResources(projectId = projectId, pipelineId = pipelineId))
        resources.addAll(collectPipelineLabelResources(projectId = projectId, pipelineId = pipelineId))
        collectPipelineTemplateResource(projectId = projectId, pipelineId = pipelineId)?.let { resources.add(it) }

        val refs = mutableSetOf<PipelineDependentResourceRef>()
        val model = pipelineRepositoryService.getModel(
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (model != null) {
            refs.addAll(
                collectModelResourceRefs(
                    projectId = projectId,
                    model = model,
                    variables = getContextMap(model)
                )
            )
        }
        resources.addAll(resolveResourceRefs(userId = userId, refs = refs))
        return resources
    }

    fun analysisDirectSubPipelineDependency(
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        return subPipelineRefService.list(
            projectId = projectId,
            pipelineId = pipelineId
        ).mapNotNull { ref ->
            val subProjectId = ref.subProjectId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val subPipelineId = ref.subPipelineId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PipelineDependentResource(
                projectId = subProjectId,
                resourceType = PipelineDependentResourceType.PIPELINE,
                resourceId = subPipelineId,
                resourceName = ref.subPipelineName?.takeIf { it.isNotBlank() } ?: subPipelineId
            )
        }.toSet()
    }

    private fun collectPipelineGroupResources(
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        val viewGroupRecords = pipelineViewGroupDao.listByPipelineId(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val viewIds = viewGroupRecords.map { it.viewId }.toSet()
        if (viewIds.isEmpty()) {
            return emptySet()
        }
        val viewMap = pipelineViewDao.list(
            dslContext = dslContext,
            projectId = projectId,
            viewIds = viewIds
        ).associateBy { it.id }
        return viewIds.map {
            PipelineDependentResource(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.PIPELINE_GROUP,
                resourceId = HashUtil.encodeLongId(it),
                resourceName = viewMap[it]?.name ?: it.toString()
            )
        }.toSet()
    }

    private fun collectPipelineLabelResources(
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        val labelPipelineRecords = pipelineLabelPipelineDao.listLabels(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
        val labelIds = labelPipelineRecords.map { it.labelId }.toSet()
        if (labelIds.isEmpty()) {
            return emptySet()
        }
        val labelMap = pipelineLabelDao.getByIds(
            dslContext = dslContext,
            projectId = projectId,
            ids = labelIds
        ).associateBy { it.id }
        return labelIds.map {
            PipelineDependentResource(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.PIPELINE_LABEL,
                resourceId = HashUtil.encodeLongId(it),
                resourceName = labelMap[it]?.name ?: it.toString()
            )
        }.toSet()
    }

    private fun collectPipelineTemplateResource(
        projectId: String,
        pipelineId: String
    ): PipelineDependentResource? {
        val pipelineTemplateRelated = pipelineTemplateRelatedService.get(
            projectId = projectId,
            pipelineId = pipelineId
        )
        if (pipelineTemplateRelated == null ||
            pipelineTemplateRelated.instanceType != PipelineInstanceTypeEnum.CONSTRAINT
        ) {
            return null
        }
        val template = pipelineTemplateInfoService.getOrNull(
            projectId = projectId,
            templateId = pipelineTemplateRelated.templateId
        ) ?: return null
        return PipelineDependentResource(
            projectId = projectId,
            resourceType = PipelineDependentResourceType.PIPELINE_TEMPLATE,
            resourceId = pipelineTemplateRelated.templateId,
            resourceName = template.name
        )
    }

    private fun collectModelResourceRefs(
        projectId: String,
        model: Model,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        model.stages.forEachIndexed { index, stage ->
            if (!stage.stageEnabled()) {
                return@forEachIndexed
            }
            if (index == 0) {
                refs.addAll(collectTriggerContainerRefs(projectId = projectId, stage = stage, variables = variables))
            } else {
                refs.addAll(collectBuildContainerRefs(projectId = projectId, stage = stage, variables = variables))
                refs.addAll(collectCheckoutElementRefs(projectId = projectId, stage = stage, variables = variables))
                refs.addAll(collectMarketElementRefs(projectId = projectId, stage = stage, variables = variables))
            }
        }
        return refs
    }

    @Suppress("NestedBlockDepth")
    private fun collectTriggerContainerRefs(
        projectId: String,
        stage: Stage,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        stage.containers.filterIsInstance<TriggerContainer>().forEach { container ->
            refs.addAll(collectParamRefs(projectId = projectId, container = container))
            container.elements.filterIsInstance<WebHookTriggerElement>().forEach e@{ element ->
                if (!element.elementEnabled()) {
                    return@e
                }
                try {
                    val repositoryConfig = RepositoryConfigUtils.buildWebhookConfig(
                        element = element,
                        variables = variables
                    ).third
                    repositoryConfig.toResourceRef(projectId = projectId)?.let { refs.add(it) }
                } catch (ignored: Exception) {
                    logger.warn("analysis trigger repository dependency failed|$projectId|${element.id}", ignored)
                }
            }
        }
        return refs
    }

    private fun collectParamRefs(
        projectId: String,
        container: TriggerContainer
    ): Set<PipelineDependentResourceRef> {
        return container.params.mapNotNull { param ->
            if (param.type != BuildFormPropertyType.SVN_TAG && param.type != BuildFormPropertyType.GIT_REF) {
                return@mapNotNull null
            }
            val repoHashId = param.repoHashId?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PipelineDependentResourceRef(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.REPOSITORY,
                refType = PipelineDependentResourceRefType.ID,
                refValue = repoHashId
            )
        }.toSet()
    }

    private fun collectBuildContainerRefs(
        projectId: String,
        stage: Stage,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        stage.containers.filterIsInstance<VMBuildContainer>().forEach { container ->
            if (!container.containerEnabled()) {
                return@forEach
            }
            val dispatchType = container.dispatchType ?: return@forEach
            when (dispatchType) {
                is ThirdPartyAgentEnvDispatchType -> {
                    getEnvironmentRef(projectId = projectId, dispatchType = dispatchType, variables = variables)?.let {
                        refs.add(it)
                    }
                    getCredentialRef(
                        projectId = projectId,
                        dockerInfo = dispatchType.dockerInfo,
                        variables = variables
                    )?.let { refs.add(it) }
                }

                is ThirdPartyAgentIDDispatchType -> {
                    getNodeRef(projectId = projectId, dispatchType = dispatchType, variables = variables)?.let {
                        refs.add(it)
                    }
                    getCredentialRef(
                        projectId = projectId,
                        dockerInfo = dispatchType.dockerInfo,
                        variables = variables
                    )?.let { refs.add(it) }
                }
            }
        }
        return refs
    }

    @Suppress("NestedBlockDepth")
    private fun collectCheckoutElementRefs(
        projectId: String,
        stage: Stage,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        stage.containers.forEach c@{ container ->
            if (container is TriggerContainer || !container.containerEnabled()) {
                return@c
            }
            container.elements.forEach e@{ element ->
                if (!element.elementEnabled()) {
                    return@e
                }
                try {
                    val repositoryConfig = getCheckoutRepositoryConfig(element = element, variables = variables)
                        ?: return@e
                    repositoryConfig.toResourceRef(projectId = projectId)?.let { refs.add(it) }
                } catch (ignored: Exception) {
                    logger.warn("analysis checkout repository dependency failed|$projectId|${element.id}", ignored)
                }
            }
        }
        return refs
    }

    private fun getCheckoutRepositoryConfig(
        element: Element,
        variables: Map<String, String>
    ): RepositoryConfig? {
        return when {
            element is CodeGitElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            element is CodeSvnElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            element is CodeGitlabElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            element is GithubElement -> RepositoryConfig(
                repositoryHashId = element.repositoryHashId,
                repositoryName = EnvUtils.parseEnv(element.repositoryName, variables),
                repositoryType = element.repositoryType ?: RepositoryType.ID
            )
            element is MarketBuildAtomElement && element.getAtomCode() in REPO_CHECKOUT_ATOM_CODES -> {
                val input = element.data["input"] as? Map<*, *> ?: return null
                getMarketBuildRepoConfig(input = input, variables = variables)
            }
            else -> null
        }
    }

    private fun getMarketBuildRepoConfig(input: Map<*, *>, variables: Map<String, String>): RepositoryConfig? {
        val taskRepoType = input["repositoryType"] as? String
        if (taskRepoType == "URL" || taskRepoType == "SELF") {
            return null
        }
        val repositoryType = try {
            RepositoryType.parseType(taskRepoType)
        } catch (ignored: Exception) {
            logger.warn("get market build repo config failed|$taskRepoType", ignored)
            return null
        }
        val repositoryId = if (repositoryType == RepositoryType.ID) {
            EnvUtils.parseEnv(input["repositoryHashId"] as? String, variables)
        } else {
            EnvUtils.parseEnv(input["repositoryName"] as? String, variables)
        }
        if (repositoryId.isBlank()) {
            return null
        }
        return RepositoryConfigUtils.buildConfig(repositoryId, repositoryType)
    }

    private fun collectMarketElementRefs(
        projectId: String,
        stage: Stage,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        stage.containers.forEach c@{ container ->
            if (container is TriggerContainer || !container.containerEnabled()) {
                return@c
            }
            container.elements.forEach e@{ element ->
                if (!element.elementEnabled()) {
                    return@e
                }
                val input = when (element) {
                    is MarketBuildAtomElement -> {
                        element.data["input"] as? Map<*, *> ?: return@e
                    }

                    is MarketBuildLessAtomElement -> {
                        element.data["input"] as? Map<*, *> ?: return@e
                    }

                    else -> return@c
                }
                try {
                    when (element.getAtomCode()) {
                        JOB_SCRIPT_EXECUTION_ATOM_CODE -> getJobScriptExecutionResourceRef(
                            projectId = projectId,
                            input = input,
                            variables = variables
                        )
                        JOB_PUSH_FILE_ATOM_CODE -> getJobPushFileResourceRef(
                            projectId = projectId,
                            input = input,
                            variables = variables
                        )
                        else -> null
                    }?.let { refs.add(it) }
                } catch (ignored: Exception) {
                    logger.warn(
                        "analysis market element dependent resource failed|" +
                                "$projectId|${element.id}|${element.getAtomCode()}",
                        ignored
                    )
                }
            }
        }
        return refs
    }

    private fun getJobScriptExecutionResourceRef(
        projectId: String,
        input: Map<*, *>,
        variables: Map<String, String>
    ): PipelineDependentResourceRef? {
        val (resourceType, refType, valueField) = when (input["envType"]?.toString()) {
            "ENV" -> Triple(
                PipelineDependentResourceType.DEPLOY_ENV,
                PipelineDependentResourceRefType.ID,
                "envId"
            )

            "ENV_NAME" -> Triple(
                PipelineDependentResourceType.DEPLOY_ENV,
                PipelineDependentResourceRefType.NAME,
                "envName"
            )

            "NODE" -> Triple(
                PipelineDependentResourceType.DEPLOY_NODE,
                PipelineDependentResourceRefType.ID,
                "nodeId"
            )

            "NODE_NAME" -> Triple(
                PipelineDependentResourceType.DEPLOY_NODE,
                PipelineDependentResourceRefType.NAME,
                "nodeName"
            )

            else -> return null
        }
        val refValue = EnvUtils.parseEnv(input[valueField]?.toString(), variables).takeIf { it.isNotBlank() }
            ?: return null
        return PipelineDependentResourceRef(
            projectId = projectId,
            resourceType = resourceType,
            refType = refType,
            refValue = refValue
        )
    }

    private fun getJobPushFileResourceRef(
        projectId: String,
        input: Map<*, *>,
        variables: Map<String, String>
    ): PipelineDependentResourceRef? {
        val (resourceType, refType, valueField) = when (input["targetEnvType"]?.toString()) {
            "ENV" -> Triple(
                PipelineDependentResourceType.DEPLOY_ENV,
                PipelineDependentResourceRefType.ID,
                "targetEnvId"
            )

            "ENV_NAME" -> Triple(
                PipelineDependentResourceType.DEPLOY_ENV,
                PipelineDependentResourceRefType.NAME,
                "targetEnvName"
            )

            "NODE" -> Triple(
                PipelineDependentResourceType.DEPLOY_NODE,
                PipelineDependentResourceRefType.ID,
                "targetNodeId"
            )

            "NODE_NAME" -> Triple(
                PipelineDependentResourceType.DEPLOY_NODE,
                PipelineDependentResourceRefType.NAME,
                "targetNodeName"
            )

            else -> return null
        }
        val refValue = EnvUtils.parseEnv(input[valueField]?.toString(), variables).takeIf { it.isNotBlank() }
            ?: return null
        return PipelineDependentResourceRef(
            projectId = projectId,
            resourceType = resourceType,
            refType = refType,
            refValue = refValue
        )
    }

    private fun RepositoryConfig.toResourceRef(projectId: String): PipelineDependentResourceRef? {
        val refValue = try {
            getRepositoryId()
        } catch (ignored: Exception) {
            logger.warn(
                "get repository id failed|$projectId|$repositoryType|$repositoryHashId|$repositoryName",
                ignored
            )
            return null
        }
        if (refValue.isBlank()) {
            return null
        }
        return PipelineDependentResourceRef(
            projectId = projectId,
            resourceType = PipelineDependentResourceType.REPOSITORY,
            refType = referenceType(repositoryType),
            refValue = refValue
        )
    }

    private fun getEnvironmentRef(
        projectId: String,
        dispatchType: ThirdPartyAgentEnvDispatchType,
        variables: Map<String, String>
    ): PipelineDependentResourceRef? {
        return if (dispatchType.agentType == AgentType.ID) {
            PipelineDependentResourceRef(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.BUILD_ENV,
                refType = PipelineDependentResourceRefType.ID,
                refValue = dispatchType.value
            )
        } else {
            val envProjectId = EnvUtils.parseEnv(dispatchType.envProjectId, variables).takeUnless { it.isBlank() }
                ?: projectId
            val envValue = EnvUtils.parseEnv(dispatchType.value, variables)
            PipelineDependentResourceRef(
                projectId = envProjectId,
                resourceType = PipelineDependentResourceType.BUILD_ENV,
                refType = PipelineDependentResourceRefType.NAME,
                refValue = envValue
            )
        }
    }

    private fun getNodeRef(
        projectId: String,
        dispatchType: ThirdPartyAgentIDDispatchType,
        variables: Map<String, String>
    ): PipelineDependentResourceRef? {
        return if (dispatchType.idType()) {
            PipelineDependentResourceRef(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.BUILD_NODE,
                refType = PipelineDependentResourceRefType.ID,
                refValue = dispatchType.displayName
            )
        } else {
            val nodeValue = EnvUtils.parseEnv(
                dispatchType.displayName,
                variables
            ).takeIf { it.isNotBlank() } ?: return null
            PipelineDependentResourceRef(
                projectId = projectId,
                resourceType = PipelineDependentResourceType.BUILD_NODE,
                refType = PipelineDependentResourceRefType.NAME,
                refValue = nodeValue
            )
        }
    }

    private fun getCredentialRef(
        projectId: String,
        dockerInfo: ThirdPartyAgentDockerInfo?,
        variables: Map<String, String>
    ): PipelineDependentResourceRef? {
        val credentialId = dockerInfo?.credential?.credentialId
        val credentialProjectId = dockerInfo?.credential?.credentialProjectId
        val finalCredentialId = EnvUtils.parseEnv(credentialId, variables).takeIf { it.isNotBlank() } ?: return null
        val finalProjectId = EnvUtils.parseEnv(credentialProjectId, variables).takeIf { it.isNotBlank() } ?: projectId
        return PipelineDependentResourceRef(
            projectId = finalProjectId,
            resourceType = PipelineDependentResourceType.CREDENTIAL,
            refType = PipelineDependentResourceRefType.ID,
            refValue = finalCredentialId
        )
    }

    private fun resolveResourceRefs(
        userId: String,
        refs: Set<PipelineDependentResourceRef>
    ): Set<PipelineDependentResource> {
        val resources = mutableSetOf<PipelineDependentResource>()
        refs.forEach { ref ->
            when (ref.resourceType) {
                PipelineDependentResourceType.REPOSITORY -> {
                    resources.addAll(resolveRepositoryRef(userId = userId, ref = ref))
                }

                PipelineDependentResourceType.BUILD_ENV,
                PipelineDependentResourceType.DEPLOY_ENV -> {
                    resolveEnvironmentRef(userId = userId, ref = ref)?.let { resources.add(it) }
                }

                PipelineDependentResourceType.BUILD_NODE,
                PipelineDependentResourceType.DEPLOY_NODE -> {
                    resolveNodeRef(userId = userId, ref = ref)?.let { resources.add(it) }
                }

                PipelineDependentResourceType.CREDENTIAL -> {
                    resolveCredentialRef(userId = userId, ref = ref)?.let { resources.add(it) }
                }

                else -> {
                    logger.warn("not support dependent resource type|${ref.projectId}|${ref.resourceType}")
                }
            }
        }
        return resources
    }

    private fun resolveRepositoryRef(
        userId: String,
        ref: PipelineDependentResourceRef
    ): Set<PipelineDependentResource> {
        val repositoryType = ref.refType.toRepositoryType()
        val repository = try {
            client.get(ServiceRepositoryResource::class).get(
                projectId = ref.projectId,
                repositoryId = ref.refValue,
                repositoryType = repositoryType
            ).data
        } catch (ignored: Exception) {
            logger.warn("get repository info failed|${ref.projectId}|${ref.refValue}|$repositoryType", ignored)
            null
        } ?: return emptySet()

        val resources = mutableSetOf(
            PipelineDependentResource(
                projectId = ref.projectId,
                resourceType = ref.resourceType,
                resourceId = repository.repoHashId!!,
                resourceName = repository.aliasName
            )
        )
        val credentialId = repository.getCredentialId()
        if (!credentialId.isNullOrBlank()) {
            resolveCredentialRef(
                userId = userId,
                ref = PipelineDependentResourceRef(
                    projectId = repository.projectId ?: ref.projectId,
                    resourceType = PipelineDependentResourceType.CREDENTIAL,
                    refType = PipelineDependentResourceRefType.ID,
                    refValue = credentialId
                )
            )?.let { resources.add(it) }
        }
        return resources
    }

    private fun Repository.getCredentialId(): String? {
        return when {
            this is CodeGitRepository && this.authType != RepoAuthType.OAUTH -> credentialId
            this is CodeTGitRepository && this.authType != RepoAuthType.OAUTH -> credentialId
            this is CodeGitlabRepository && this.authType != RepoAuthType.OAUTH -> credentialId
            this is ScmGitRepository && this.authType != RepoAuthType.OAUTH -> credentialId
            this is CodeSvnRepository -> credentialId
            this is ScmSvnRepository -> credentialId
            this is CodeP4Repository -> credentialId
            else -> null
        }
    }

    private fun resolveEnvironmentRef(userId: String, ref: PipelineDependentResourceRef): PipelineDependentResource? {
        val env = try {
            if (ref.refType == PipelineDependentResourceRefType.ID) {
                client.get(ServiceEnvironmentResource::class).get(
                    userId = userId,
                    projectId = ref.projectId,
                    envHashId = ref.refValue
                ).data
            } else {
                client.get(ServiceEnvironmentResource::class).getByName(
                    userId = userId,
                    projectId = ref.projectId,
                    envName = ref.refValue
                ).data
            }
        } catch (ignored: Exception) {
            logger.warn("get environment info failed|${ref.projectId}|${ref.refType}|${ref.refValue}", ignored)
            null
        } ?: return null
        return PipelineDependentResource(
            projectId = ref.projectId,
            resourceType = ref.resourceType,
            resourceId = env.envHashId,
            resourceName = env.name
        )
    }

    private fun resolveNodeRef(userId: String, ref: PipelineDependentResourceRef): PipelineDependentResource? {
        val node = try {
            if (ref.refType == PipelineDependentResourceRefType.ID) {
                if (ref.resourceType == PipelineDependentResourceType.BUILD_NODE) {
                    client.get(ServiceNodeResource::class).getNodeStatus(
                        userId = userId,
                        projectId = ref.projectId,
                        nodeHashId = null,
                        nodeName = null,
                        agentHashId = ref.refValue
                    ).data
                } else {
                    client.get(ServiceNodeResource::class).getNodeStatus(
                        userId = userId,
                        projectId = ref.projectId,
                        nodeHashId = ref.refValue,
                        nodeName = null,
                        agentHashId = null
                    ).data
                }
            } else {
                client.get(ServiceNodeResource::class).getNodeStatus(
                    userId = userId,
                    projectId = ref.projectId,
                    nodeHashId = null,
                    nodeName = ref.refValue,
                    agentHashId = null
                ).data
            }
        } catch (ignored: Exception) {
            logger.warn("get node info failed|${ref.projectId}|${ref.refType}|${ref.refValue}", ignored)
            null
        } ?: return null
        return PipelineDependentResource(
            projectId = ref.projectId,
            resourceType = ref.resourceType,
            // 部署节点,流水线使用的是agentHashId
            resourceId = if (ref.resourceType == PipelineDependentResourceType.BUILD_NODE) {
                node.agentHashId!!
            } else {
                node.nodeHashId
            },
            resourceName = node.displayName!!
        )
    }

    private fun resolveCredentialRef(userId: String, ref: PipelineDependentResourceRef): PipelineDependentResource? {
        val credential = try {
            client.get(ServiceCredentialResource::class).getBasicInfo(
                userId = userId,
                projectId = ref.projectId,
                credentialId = ref.refValue
            ).data
        } catch (ignored: Exception) {
            logger.warn("get credential info failed|${ref.projectId}|${ref.refValue}", ignored)
            return null
        } ?: return null
        return PipelineDependentResource(
            projectId = ref.projectId,
            resourceType = ref.resourceType,
            resourceId = credential.credentialId,
            resourceName = credential.credentialName ?: credential.credentialId
        )
    }

    private fun getContextMap(model: Model): Map<String, String> {
        val triggerContainer = model.stages.firstOrNull()?.containers?.firstOrNull() as? TriggerContainer
            ?: return emptyMap()
        val variables = triggerContainer.params.associate { param ->
            param.id to param.defaultValue.toString()
        }
        return PipelineVarUtil.fillVariableMap(variables)
    }

    private fun referenceType(repositoryType: RepositoryType): PipelineDependentResourceRefType {
        return if (repositoryType == RepositoryType.ID) {
            PipelineDependentResourceRefType.ID
        } else {
            PipelineDependentResourceRefType.NAME
        }
    }

    private fun PipelineDependentResourceRefType.toRepositoryType(): RepositoryType {
        return if (this == PipelineDependentResourceRefType.ID) {
            RepositoryType.ID
        } else {
            RepositoryType.NAME
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDependencyAnalyzeService::class.java)

        private val REPO_CHECKOUT_ATOM_CODES = setOf(
            "gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo"
        )
        private const val JOB_PUSH_FILE_ATOM_CODE = "JobPushFile"
        private const val JOB_SCRIPT_EXECUTION_ATOM_CODE = "JobScriptExecutionA"
    }
}
