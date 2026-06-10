package com.tencent.devops.process.service.task.copy

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.TemplateInstanceDescriptor
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.PipelineModelAndSetting
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.SubPipelineCallElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeGitlabElement
import com.tencent.devops.common.pipeline.pojo.element.agent.CodeSvnElement
import com.tencent.devops.common.pipeline.pojo.element.agent.GithubElement
import com.tencent.devops.common.pipeline.pojo.element.atom.SubPipelineType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGithubWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeGitlabWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerData
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeP4WebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeSVNWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeScmSvnWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.CodeTGitWebHookTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.WebHookTriggerElement
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDockerInfo
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentEnvDispatchType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentIDDispatchType
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineDependentResource
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.service.template.v2.PipelineTemplateSettingService
import com.tencent.devops.process.utils.PipelineVarUtil
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线依赖资源替换
 */
@Service
class PipelineDependencyReplaceService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineTemplateSettingService: PipelineTemplateSettingService
) {

    fun replacePipelineResourceDependency(
        userId: String,
        projectId: String,
        pipelineId: String,
        targetProjectId: String,
        targetPipelineId: String,
        targetPipelineName: String,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): PipelineModelAndSetting {
        pipelineRepositoryService.getPipelineInfo(projectId = projectId, pipelineId = pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = null,
            includeDraft = true
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val setting = pipelineSettingFacadeService.userGetSetting(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            version = resource.settingVersion ?: 0
        )
        val context = ReplaceContext(replaceResourceMap = replaceResourceMap)
        val replacedModel = resource.model.copy(
            stages = replaceStages(
                projectId = projectId,
                stages = resource.model.stages,
                context = context
            ),
            template = replaceTemplate(
                template = resource.model.template,
                context = context
            )
        ).copy(
            name = targetPipelineName
        )
        val replaceSetting = setting.copy(
            projectId = targetProjectId,
            pipelineId = targetPipelineId,
            pipelineName = targetPipelineName
        )
        if (context.missingResources.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_MIGRATE_FAILED,
                params = arrayOf(formatMissingResources(context.missingResources))
            )
        }
        return PipelineModelAndSetting(model = replacedModel, setting = replaceSetting)
    }

    fun replaceTemplateResourceDependency(
        sourceProjectId: String,
        sourceTemplateId: String,
        sourceTemplateVersion: Long,
        sourceTemplateResource: PipelineTemplateResource,
        targetProjectId: String,
        targetTemplateId: String,
        targetTemplateName: String,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): PipelineModelAndSetting {
        val sourceModel = sourceTemplateResource.model
        if (sourceModel !is Model) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_SOURCE_RESOURCE_NOT_EXISTS,
                params = arrayOf(sourceProjectId, "$sourceTemplateId@$sourceTemplateVersion")
            )
        }
        val sourceSetting = pipelineTemplateSettingService.get(
            projectId = sourceProjectId,
            templateId = sourceTemplateId,
            settingVersion = sourceTemplateResource.settingVersion
        )
        val context = ReplaceContext(replaceResourceMap = replaceResourceMap)
        val replacedModel = sourceModel.copy(
            stages = replaceStages(
                projectId = sourceProjectId,
                stages = sourceModel.stages,
                context = context
            )
        ).copy(
            name = targetTemplateName
        )
        val replaceSetting = sourceSetting.copy(
            projectId = targetProjectId,
            pipelineId = targetTemplateId,
            pipelineName = targetTemplateName
        )
        if (context.missingResources.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_COPY_RESOURCE_MIGRATE_FAILED,
                params = arrayOf(formatMissingResources(context.missingResources))
            )
        }
        return PipelineModelAndSetting(model = replacedModel, setting = replaceSetting)
    }

    private fun replaceTemplate(
        template: TemplateInstanceDescriptor?,
        context: ReplaceContext
    ): TemplateInstanceDescriptor? {
        if (template == null || template.templateId.isNullOrBlank()) {
            return null
        }
        val targetTemplateId = context.getTargetResource(
            resourceType = PipelineDependentResourceType.REPOSITORY,
            resourceId = template.templateId!!
        )?.resourceId
        return template.copy(templateId = targetTemplateId)
    }

    private fun replaceStages(
        projectId: String,
        stages: List<Stage>,
        context: ReplaceContext
    ): List<Stage> {
        return stages.map { stage ->
            stage.copy(
                containers = replaceContainers(
                    projectId = projectId,
                    containers = stage.containers,
                    context = context
                )
            )
        }
    }

    private fun replaceContainers(
        projectId: String,
        containers: List<Container>,
        context: ReplaceContext
    ): List<Container> {
        return containers.map { container ->
            when (container) {
                is TriggerContainer -> {
                    container.copy(
                        params = replaceParams(params = container.params, context = context),
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            context = context
                        )
                    )
                }

                is VMBuildContainer -> {
                    val replaceContainer = replaceBuildContainer(
                        container = container,
                        context = context
                    )
                    replaceContainer.copy(
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            context = context
                        )
                    )
                }

                is NormalContainer -> {
                    container.copy(
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            context = context
                        )
                    )
                }

                else -> container
            }
        }
    }

    private fun replaceElements(
        projectId: String,
        elements: List<Element>,
        context: ReplaceContext
    ): List<Element> {
        return elements.map { element ->
            when {
                element is WebHookTriggerElement -> {
                    replaceTriggerRepository(
                        element = element,
                        context = context
                    )
                }

                isRepoCheckoutElement(element) -> {
                    replaceCheckoutRepository(
                        element = element,
                        context = context
                    )
                }

                element is SubPipelineCallElement && element.isIdSubPipelineType() -> {
                    context.getTargetResource(
                        resourceType = PipelineDependentResourceType.PIPELINE,
                        resourceId = element.subPipelineId
                    )?.let {
                        element.copy(subPipelineId = it.resourceId)
                    } ?: element
                }

                element is MarketBuildAtomElement -> {
                    replaceMarketBuildAtomElement(
                        projectId = projectId,
                        element = element,
                        context = context
                    )
                }

                element is MarketBuildLessAtomElement -> {
                    replaceMarketBuildLessAtomElement(
                        projectId = projectId,
                        element = element,
                        context = context
                    )
                }

                else -> element
            }
        }
    }

    private fun replaceParams(
        params: List<BuildFormProperty>,
        context: ReplaceContext
    ): List<BuildFormProperty> {
        return params.map { param ->
            if (param.type != BuildFormPropertyType.SVN_TAG && param.type != BuildFormPropertyType.GIT_REF) {
                return@map param
            }
            val targetRepositoryId = getTargetRepositoryId(
                repositoryHashId = param.repoHashId,
                context = context
            ) ?: return@map param
            param.copy(repoHashId = targetRepositoryId)
        }
    }

    private fun replaceBuildContainer(
        container: VMBuildContainer,
        context: ReplaceContext
    ): VMBuildContainer {
        val dispatchType = container.dispatchType as? ThirdPartyAgentDispatch ?: return container
        val replacedDispatchType = when {
            dispatchType is ThirdPartyAgentEnvDispatchType &&
                dispatchType.agentType == AgentType.ID -> replaceEnvDispatchType(
                dispatchType = dispatchType,
                context = context
            )

            dispatchType is ThirdPartyAgentIDDispatchType &&
                dispatchType.idType() -> replaceNodeDispatchType(
                dispatchType = dispatchType,
                context = context
            )

            else -> dispatchType
        }
        return if (replacedDispatchType === container.dispatchType) {
            container
        } else {
            container.copy(dispatchType = replacedDispatchType)
        }
    }

    private fun replaceEnvDispatchType(
        dispatchType: ThirdPartyAgentEnvDispatchType,
        context: ReplaceContext
    ): ThirdPartyAgentEnvDispatchType {
        val targetEnvId = context.getTargetResource(
            resourceType = PipelineDependentResourceType.BUILD_ENV,
            resourceId = dispatchType.value
        )?.resourceId
        val dockerInfo = replaceDockerInfo(
            dockerInfo = dispatchType.dockerInfo,
            context = context
        )
        if (targetEnvId == null && dockerInfo === dispatchType.dockerInfo) {
            return dispatchType
        }
        return dispatchType.copy(
            envName = targetEnvId ?: dispatchType.envName,
            dockerInfo = dockerInfo
        )
    }

    private fun replaceNodeDispatchType(
        dispatchType: ThirdPartyAgentIDDispatchType,
        context: ReplaceContext
    ): ThirdPartyAgentIDDispatchType {
        val targetNodeId = context.getTargetResource(
            resourceType = PipelineDependentResourceType.BUILD_NODE,
            resourceId = dispatchType.displayName
        )?.resourceId
        val dockerInfo = replaceDockerInfo(
            dockerInfo = dispatchType.dockerInfo,
            context = context
        )
        if (targetNodeId == null && dockerInfo === dispatchType.dockerInfo) {
            return dispatchType
        }
        return dispatchType.copy(
            displayName = targetNodeId ?: dispatchType.displayName,
            dockerInfo = dockerInfo
        )
    }

    private fun replaceDockerInfo(
        dockerInfo: ThirdPartyAgentDockerInfo?,
        context: ReplaceContext
    ): ThirdPartyAgentDockerInfo? {
        val credential = dockerInfo?.credential ?: return dockerInfo
        val credentialId = credential.credentialId?.takeIf { it.isNotBlank() } ?: return dockerInfo
        if (PipelineVarUtil.isVar(credentialId)) {
            return dockerInfo
        }
        val targetCredential = context.getTargetResource(
            resourceType = PipelineDependentResourceType.CREDENTIAL,
            resourceId = credentialId
        ) ?: return dockerInfo
        return dockerInfo.copy(
            credential = credential.copy(
                credentialId = targetCredential.resourceId,
                credentialProjectId = targetCredential.projectId
            )
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun replaceTriggerRepository(
        element: Element,
        context: ReplaceContext
    ): Element {
        return when {
            element is CodeGitWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeSVNWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGitlabWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGithubWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeTGitWebHookTriggerElement &&
                element.data.input.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.data.input.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(
                    data = element.data.copy(
                        input = element.data.input.copy(repositoryHashId = targetRepositoryId)
                    )
                )
            }

            element is CodeScmGitWebHookTriggerElement &&
                element.data.input.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.data.input.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(
                    data = element.data.copy(
                        input = element.data.input.copy(repositoryHashId = targetRepositoryId)
                    )
                )
            }

            element is CodeScmSvnWebHookTriggerElement &&
                element.data.input.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.data.input.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(
                    data = element.data.copy(
                        input = element.data.input.copy(repositoryHashId = targetRepositoryId)
                    )
                )
            }

            element is CodeP4WebHookTriggerElement &&
                element.data.input.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.data.input.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(
                    data = CodeP4WebHookTriggerData(
                        input = element.data.input.copy(repositoryHashId = targetRepositoryId)
                    )
                )
            }

            else -> element
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun replaceCheckoutRepository(
        element: Element,
        context: ReplaceContext
    ): Element {
        return when {
            element is CodeGitElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeSvnElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGitlabElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is GithubElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    context = context
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is MarketBuildAtomElement && element.getAtomCode() in REPO_CHECKOUT_ATOM_CODES -> {
                val input = element.data["input"]
                if (input !is Map<*, *>) return element
                val repositoryType = input["repositoryType"] as String?
                val repositoryHashId = input["repositoryHashId"] as String?
                if (repositoryType == RepositoryType.ID.name && !repositoryHashId.isNullOrBlank()) {
                    val targetRepositoryId = getTargetRepositoryId(
                        repositoryHashId = repositoryHashId,
                        context = context
                    ) ?: return element
                    val replacedInput = replaceInputField(
                        input = input,
                        fieldName = REPOSITORY_HASH_ID,
                        value = targetRepositoryId
                    )
                    element.copy(
                        data = replaceInput(
                            data = element.data,
                            input = replacedInput
                        )
                    )
                } else {
                    element
                }
            }

            else -> element
        }
    }

    private fun isRepoCheckoutElement(element: Element): Boolean {
        return element is CodeGitElement ||
            element is CodeSvnElement ||
            element is CodeGitlabElement ||
            element is GithubElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() in REPO_CHECKOUT_ATOM_CODES)
    }

    private fun replaceMarketBuildAtomElement(
        projectId: String,
        element: MarketBuildAtomElement,
        context: ReplaceContext
    ): Element {
        val input = element.data[INPUT]
        if (input !is Map<*, *>) {
            return element
        }
        return when (element.getAtomCode()) {
            SUB_PIPELINE_EXEC_ATOM_CODE -> {
                val replacedInput = replaceSubPipelineExecInput(
                    projectId = projectId, input = input, context = context
                ) ?: return element
                element.copy(data = replaceInput(data = element.data, input = replacedInput))
            }

            else -> {
                element
            }
        }
    }

    private fun replaceMarketBuildLessAtomElement(
        projectId: String,
        element: MarketBuildLessAtomElement,
        context: ReplaceContext
    ): Element {
        val input = element.data[INPUT]
        if (input !is Map<*, *>) {
            return element
        }
        return when (element.getAtomCode()) {
            SUB_PIPELINE_EXEC_ATOM_CODE -> {
                val replacedInput = replaceSubPipelineExecInput(
                    projectId = projectId, input = input, context = context
                ) ?: return element
                element.copy(data = replaceInput(data = element.data, input = replacedInput))
            }

            JOB_SCRIPT_EXECUTION_ATOM_CODE -> {
                val replacedInput = replaceJobScriptExecutionInput(
                    input = input,
                    context = context
                ) ?: return element
                element.copy(data = replaceInput(data = element.data, input = replacedInput))
            }

            JOB_PUSH_FILE_ATOM_CODE -> {
                val replacedInput = replaceJobPushFileInput(
                    input = input,
                    context = context
                ) ?: return element
                element.copy(data = replaceInput(data = element.data, input = replacedInput))
            }

            else -> {
                element
            }
        }
    }

    private fun replaceSubPipelineExecInput(
        projectId: String,
        input: Map<*, *>,
        context: ReplaceContext
    ): Map<*, *>? {
        val subProjectId = input["projectId"]?.toString()?.ifBlank { projectId } ?: projectId
        val subPipelineTypeStr = input.getOrDefault("subPipelineType", SubPipelineType.ID.name)
        val subPipelineId = input[SUB_PIPELINE_EXEC_ID]?.toString()
        if (subProjectId != projectId ||
            subPipelineTypeStr != SubPipelineType.ID.name ||
            subPipelineId.isNullOrBlank()
        ) {
            return null
        }
        val targetPipeline = context.getTargetResource(
            resourceType = PipelineDependentResourceType.PIPELINE,
            resourceId = subPipelineId
        ) ?: return null
        return replaceInputField(
            input = input,
            fieldName = SUB_PIPELINE_EXEC_ID,
            value = targetPipeline.resourceId
        )
    }

    private fun replaceJobScriptExecutionInput(
        input: Map<*, *>,
        context: ReplaceContext
    ): Map<*, *>? {
        if (input[JOB_SCRIPT_EXECUTION_ENV_TYPE]?.toString() != ENV) {
            return null
        }
        return replaceDeployEnvInput(
            input = input,
            fieldName = JOB_SCRIPT_EXECUTION_ENV_ID,
            context = context
        )
    }

    private fun replaceJobPushFileInput(
        input: Map<*, *>,
        context: ReplaceContext
    ): Map<*, *>? {
        if (input[JOB_PUSH_FILE_TARGET_ENV_TYPE]?.toString() != ENV) {
            return null
        }
        return replaceDeployEnvInput(
            input = input,
            fieldName = JOB_PUSH_FILE_TARGET_ENV_ID,
            context = context
        )
    }

    private fun replaceDeployEnvInput(
        input: Map<*, *>,
        fieldName: String,
        context: ReplaceContext
    ): Map<*, *>? {
        val envId = input[fieldName]?.let {
            JsonUtil.anyToOrNull(it, object : TypeReference<List<String>>() {})
        }?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val targetEnv = context.getTargetResource(
            resourceType = PipelineDependentResourceType.DEPLOY_ENV,
            resourceId = envId
        ) ?: return null
        return replaceInputField(
            input = input,
            fieldName = fieldName,
            value = listOf(targetEnv.resourceId)
        )
    }

    private fun replaceInputField(
        input: Map<*, *>,
        fieldName: String,
        value: Any
    ): Map<*, *> {
        val newInput = input.toMutableMap()
        newInput[fieldName] = value
        return newInput
    }

    private fun replaceInput(
        data: Map<String, Any>,
        input: Map<*, *>
    ): Map<String, Any> {
        return data.toMutableMap().apply {
            this[INPUT] = input
        }
    }

    private fun getTargetRepositoryId(
        repositoryHashId: String?,
        context: ReplaceContext
    ): String? {
        val sourceRepositoryHashId = repositoryHashId?.takeIf { it.isNotBlank() } ?: return null
        return context.getTargetResource(
            resourceType = PipelineDependentResourceType.REPOSITORY,
            resourceId = sourceRepositoryHashId
        )?.resourceId
    }

    private fun SubPipelineCallElement.isIdSubPipelineType(): Boolean {
        return subPipelineType == null || subPipelineType == SubPipelineType.ID
    }

    private fun formatMissingResources(missingResources: Set<MissingReplaceResource>): String {
        return missingResources.joinToString(",") {
            "${it.resourceType.name}:${it.resourceId}"
        }
    }

    private data class ReplaceContext(
        val replaceResourceMap: Map<String, PipelineDependentResource>,
        val missingResources: MutableSet<MissingReplaceResource> = mutableSetOf()
    ) {
        fun getTargetResource(
            resourceType: PipelineDependentResourceType,
            resourceId: String
        ): PipelineDependentResource? {
            val sourceResourceId = resourceId.takeIf { it.isNotBlank() } ?: return null
            return replaceResourceMap["${resourceType}_$sourceResourceId"] ?: run {
                missingResources.add(
                    MissingReplaceResource(
                        resourceType = resourceType,
                        resourceId = sourceResourceId
                    )
                )
                null
            }
        }
    }

    private data class MissingReplaceResource(
        val resourceType: PipelineDependentResourceType,
        val resourceId: String
    )

    companion object {
        private const val INPUT = "input"
        private const val REPOSITORY_HASH_ID = "repositoryHashId"
        private const val SUB_PIPELINE_EXEC_ID = "subPip"
        private const val ENV = "ENV"
        private const val JOB_SCRIPT_EXECUTION_ENV_TYPE = "envType"
        private const val JOB_SCRIPT_EXECUTION_ENV_ID = "envId"
        private const val JOB_PUSH_FILE_TARGET_ENV_TYPE = "targetEnvType"
        private const val JOB_PUSH_FILE_TARGET_ENV_ID = "targetEnvId"
        private val REPO_CHECKOUT_ATOM_CODES = setOf(
            "gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo"
        )
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
        private const val JOB_PUSH_FILE_ATOM_CODE = "JobPushFile"
        private const val JOB_SCRIPT_EXECUTION_ATOM_CODE = "JobScriptExecutionA"
    }
}
