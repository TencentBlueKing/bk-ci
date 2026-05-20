package com.tencent.devops.process.service

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
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
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.dao.label.PipelineLabelPipelineDao
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResourceRef
import com.tencent.devops.process.pojo.pipeline.ReferenceType
import com.tencent.devops.process.utils.PipelineVarUtil
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.ticket.api.ServiceCredentialResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
/**
 * 流水线依赖资源分析
 */
class PipelineDependentResourceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineViewGroupDao: PipelineViewGroupDao,
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineLabelPipelineDao: PipelineLabelPipelineDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    fun analysisResourceDependency(
        userId: String,
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        val resources = mutableSetOf<PipelineDependentResource>()
        resources.addAll(collectPipelineGroupResources(projectId = projectId, pipelineId = pipelineId))
        resources.addAll(collectPipelineLabelResources(projectId = projectId, pipelineId = pipelineId))

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

    fun analysisSubPipelineDependency(
        projectId: String,
        pipelineId: String
    ): Set<PipelineDependentResource> {
        val model = pipelineRepositoryService.getModel(
            projectId = projectId,
            pipelineId = pipelineId
        ) ?: return emptySet()
        val variables = getContextMap(model)
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        model.stages.forEach { stage ->
            if (!stage.stageEnabled()) {
                return@forEach
            }
            stage.containers.forEach c@{ container ->
                if (container is TriggerContainer || !container.containerEnabled()) {
                    return@c
                }
                container.elements.forEach e@{ element ->
                    if (!element.elementEnabled() || !supportSubPipelineElement(element)) {
                        return@e
                    }
                    try {
                        getSubPipelineRef(
                            projectId = projectId, element = element, contextMap = variables
                        )?.let { refs.add(it) }
                    } catch (ignored: Exception) {
                        logger.warn(
                            "analysis sub pipeline dependency failed|$projectId|$pipelineId|${element.id}",
                            ignored
                        )
                    }
                }
            }
        }
        return refs.mapNotNull { resolveSubPipelineRef(it) }.toSet()
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
                resourceType = ResourceTypeId.PIPELINE_GROUP,
                resourceId = it.toString(),
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
                resourceType = ResourceTypeId.PIPELINE_LABEL,
                resourceId = it.toString(),
                resourceName = labelMap[it]?.name ?: it.toString()
            )
        }.toSet()
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
            }
        }
        return refs
    }

    private fun collectTriggerContainerRefs(
        projectId: String,
        stage: Stage,
        variables: Map<String, String>
    ): Set<PipelineDependentResourceRef> {
        val refs = mutableSetOf<PipelineDependentResourceRef>()
        stage.containers.filterIsInstance<TriggerContainer>().forEach { container ->
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
            resourceType = ResourceTypeId.REPERTORY,
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
                resourceType = ResourceTypeId.ENVIRONMENT,
                refType = ReferenceType.ID,
                refValue = dispatchType.value
            )
        } else {
            val envProjectId = EnvUtils.parseEnv(dispatchType.envProjectId, variables).takeUnless { it.isBlank() }
                ?: projectId
            val envValue = EnvUtils.parseEnv(dispatchType.value, variables)
            PipelineDependentResourceRef(
                projectId = envProjectId,
                resourceType = ResourceTypeId.ENVIRONMENT,
                refType = ReferenceType.NAME,
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
                resourceType = ResourceTypeId.ENV_NODE,
                refType = ReferenceType.ID,
                refValue = dispatchType.displayName
            )
        } else {
            val nodeValue = EnvUtils.parseEnv(dispatchType.value, variables).takeIf { it.isNotBlank() } ?: return null
            PipelineDependentResourceRef(
                projectId = projectId,
                resourceType = ResourceTypeId.ENV_NODE,
                refType = ReferenceType.NAME,
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
            resourceType = ResourceTypeId.CREDENTIAL,
            refType = ReferenceType.ID,
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
                ResourceTypeId.REPERTORY -> resources.addAll(
                    resolveRepositoryRef(
                        userId = userId,
                        ref = ref
                    )
                )
                ResourceTypeId.ENVIRONMENT -> resolveEnvironmentRef(
                    userId = userId,
                    ref = ref
                )?.let { resources.add(it) }

                ResourceTypeId.ENV_NODE -> resolveNodeRef(
                    userId = userId, ref = ref
                )?.let { resources.add(it) }

                ResourceTypeId.CREDENTIAL -> resolveCredentialRef(
                    userId = userId, ref = ref
                )?.let { resources.add(it) }
            }
        }
        return resources
    }

    private fun resolveRepositoryRef(userId: String, ref: PipelineDependentResourceRef): Set<PipelineDependentResource> {
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
        val credentialId = repository.credentialId.takeIf { it.isNotBlank() }
        if (credentialId != null) {
            resolveCredentialRef(
                userId = userId,
                ref = PipelineDependentResourceRef(
                    projectId = repository.projectId ?: ref.projectId,
                    resourceType = ResourceTypeId.CREDENTIAL,
                    refType = ReferenceType.ID,
                    refValue = credentialId
                )
            )?.let { resources.add(it) }
        }
        return resources
    }

    private fun resolveEnvironmentRef(userId: String, ref: PipelineDependentResourceRef): PipelineDependentResource? {
        val env = try {
            if (ref.refType == ReferenceType.ID) {
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
            if (ref.refType == ReferenceType.ID) {
                client.get(ServiceNodeResource::class).getNodeStatus(
                    userId = userId,
                    projectId = ref.projectId,
                    nodeHashId = ref.refValue,
                    nodeName = null,
                    agentHashId = null
                ).data
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
            resourceId = node.nodeHashId,
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

    private fun supportSubPipelineElement(element: Element): Boolean {
        return element is SubPipelineCallElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            (element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSubPipelineRef(
        projectId: String,
        element: Element,
        contextMap: Map<String, String>
    ): PipelineDependentResourceRef? {
        return when (element) {
            is SubPipelineCallElement -> getSubPipelineCallRef(
                projectId = projectId,
                element = element,
                contextMap = contextMap
            )
            is MarketBuildAtomElement -> getSubPipelineExecRef(
                projectId = projectId,
                inputMap = element.data["input"] as? Map<String, Any> ?: return null,
                contextMap = contextMap
            )
            is MarketBuildLessAtomElement -> getSubPipelineExecRef(
                projectId = projectId,
                inputMap = element.data["input"] as? Map<String, Any> ?: return null,
                contextMap = contextMap
            )
            else -> null
        }
    }

    private fun getSubPipelineCallRef(
        projectId: String,
        element: SubPipelineCallElement,
        contextMap: Map<String, String>
    ): PipelineDependentResourceRef? {
        val subPipelineType = element.subPipelineType ?: SubPipelineType.ID
        return getSubPipelineRef(
            projectId = projectId,
            subProjectId = projectId,
            subPipelineType = subPipelineType,
            subPipelineId = element.subPipelineId,
            subPipelineName = element.subPipelineName,
            contextMap = contextMap
        )
    }

    private fun getSubPipelineExecRef(
        projectId: String,
        inputMap: Map<String, Any>,
        contextMap: Map<String, String>
    ): PipelineDependentResourceRef? {
        val subProjectId = inputMap["projectId"]?.let { projectIdStr ->
            if (projectIdStr is String && projectIdStr.isNotBlank()) projectIdStr else null
        } ?: projectId
        val subPipelineTypeStr = inputMap.getOrDefault("subPipelineType", "ID")
        val subPipelineName = inputMap["subPipelineName"]?.toString()
        val subPipelineId = inputMap["subPip"]?.toString()
        val subPipelineType = when (subPipelineTypeStr) {
            "ID" -> SubPipelineType.ID
            "NAME" -> SubPipelineType.NAME
            else -> return null
        }
        return getSubPipelineRef(
            projectId = projectId,
            subProjectId = subProjectId,
            subPipelineType = subPipelineType,
            subPipelineId = subPipelineId,
            subPipelineName = subPipelineName,
            contextMap = contextMap
        )
    }

    @SuppressWarnings("LongParameterList")
    private fun getSubPipelineRef(
        projectId: String,
        subProjectId: String,
        subPipelineType: SubPipelineType,
        subPipelineId: String?,
        subPipelineName: String?,
        contextMap: Map<String, String>
    ): PipelineDependentResourceRef? {
        return if (subPipelineType == SubPipelineType.ID) {
            if (subPipelineId.isNullOrBlank()) {
                return null
            }
            PipelineDependentResourceRef(
                projectId = subProjectId,
                resourceType = ResourceTypeId.PIPELINE,
                refType = ReferenceType.ID,
                refValue = subPipelineId
            )
        } else {
            if (subPipelineName.isNullOrBlank()) {
                return null
            }
            val finalSubProjectId = EnvUtils.parseEnv(subProjectId, contextMap)
            val finalSubPipelineName = EnvUtils.parseEnv(subPipelineName, contextMap)
            PipelineDependentResourceRef(
                projectId = finalSubProjectId,
                resourceType = ResourceTypeId.PIPELINE,
                refType = ReferenceType.NAME,
                refValue = finalSubPipelineName
            )
        }
    }

    private fun resolveSubPipelineRefs(refs: Set<PipelineDependentResourceRef>): Set<PipelineDependentResource> {
        return refs.mapNotNull { resolveSubPipelineRef(it) }.toSet()
    }

    private fun resolveSubPipelineRef(ref: PipelineDependentResourceRef): PipelineDependentResource? {
        val pipelineInfo = if (ref.refType == ReferenceType.ID) {
            pipelineInfoDao.getPipelineInfo(
                dslContext = dslContext,
                projectId = ref.projectId,
                pipelineId = ref.refValue
            )
        } else {
            pipelineInfoDao.getPipelineInfoByName(
                dslContext = dslContext,
                projectId = ref.projectId,
                pipelineName = ref.refValue,
                filterDelete = true
            )
        } ?: run {
            logger.info(
                "sub-pipeline not found|projectId:${ref.projectId}|refType:${ref.refType}|" +
                        "refValue:${ref.refValue}"
            )
            return null
        }
        return PipelineDependentResource(
            projectId = ref.projectId,
            resourceType = ResourceTypeId.PIPELINE,
            resourceId = pipelineInfo.pipelineId,
            resourceName = pipelineInfo.pipelineName
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

    private fun referenceType(repositoryType: RepositoryType): ReferenceType {
        return if (repositoryType == RepositoryType.ID) {
            ReferenceType.ID
        } else {
            ReferenceType.NAME
        }
    }

    private fun ReferenceType.toRepositoryType(): RepositoryType {
        return if (this == ReferenceType.ID) {
            RepositoryType.ID
        } else {
            RepositoryType.NAME
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDependentResourceService::class.java)
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"

        private val REPO_CHECKOUT_ATOM_CODES = setOf(
            "gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo"
        )
    }
}
