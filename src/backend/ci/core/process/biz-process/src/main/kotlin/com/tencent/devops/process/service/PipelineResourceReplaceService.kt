package com.tencent.devops.process.service

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
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
import com.tencent.devops.process.service.pipeline.PipelineSettingFacadeService
import com.tencent.devops.process.utils.PipelineVarUtil
import jakarta.ws.rs.core.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线依赖资源替换
 */
@Service
class PipelineResourceReplaceService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService
) {

    fun replaceResourceDependency(
        userId: String,
        projectId: String,
        pipelineId: String,
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
        val replacedModel = resource.model.copy(
            stages = replaceStages(
                projectId = projectId,
                stages = resource.model.stages,
                replaceResourceMap = replaceResourceMap
            )
        )
        return PipelineModelAndSetting(model = replacedModel, setting = setting)
    }

    private fun replaceStages(
        projectId: String,
        stages: List<Stage>,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): List<Stage> {
        return stages.map { stage ->
            stage.copy(
                containers = replaceContainers(
                    projectId = projectId,
                    containers = stage.containers,
                    replaceResourceMap = replaceResourceMap
                )
            )
        }
    }

    private fun replaceContainers(
        projectId: String,
        containers: List<Container>,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): List<Container> {
        return containers.map { container ->
            when (container) {
                is TriggerContainer -> {
                    container.copy(
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            replaceResourceMap = replaceResourceMap
                        )
                    )
                }

                is VMBuildContainer -> {
                    val replaceContainer = replaceBuildContainer(
                        container = container,
                        replaceResource = replaceResourceMap
                    )
                    replaceContainer.copy(
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            replaceResourceMap = replaceResourceMap
                        )
                    )
                }

                is NormalContainer -> {
                    container.copy(
                        elements = replaceElements(
                            projectId = projectId,
                            elements = container.elements,
                            replaceResourceMap = replaceResourceMap
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
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): List<Element> {
        return elements.map { element ->
            when {
                element is WebHookTriggerElement -> {
                    replaceTriggerRepository(
                        element = element,
                        replaceResourceMap = replaceResourceMap
                    )
                }

                isRepoCheckoutElement(element) -> {
                    replaceCheckoutRepository(
                        element = element,
                        replaceResourceMap = replaceResourceMap
                    )
                }

                isSubPipelineElement(element) -> {
                    replaceSubPipelineElement(
                        projectId = projectId,
                        element = element,
                        replaceResourceMap = replaceResourceMap
                    )
                }

                else -> element
            }
        }
    }

    private fun replaceBuildContainer(
        container: VMBuildContainer,
        replaceResource: Map<String, PipelineDependentResource>
    ): VMBuildContainer {
        val dispatchType = container.dispatchType as? ThirdPartyAgentDispatch ?: return container
        val replacedDispatchType = when {
            dispatchType is ThirdPartyAgentEnvDispatchType &&
                dispatchType.agentType == AgentType.ID -> replaceEnvDispatchType(
                dispatchType = dispatchType,
                replaceResource = replaceResource
            )

            dispatchType is ThirdPartyAgentIDDispatchType &&
                dispatchType.idType() -> replaceNodeDispatchType(
                dispatchType = dispatchType,
                replaceResource = replaceResource
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
        replaceResource: Map<String, PipelineDependentResource>
    ): ThirdPartyAgentEnvDispatchType {
        val resourceKey = resourceKey(
            resourceType = PipelineDependentResourceType.BUILD_ENV,
            resourceId = dispatchType.value
        )
        val targetEnvId = replaceResource[resourceKey]?.resourceId
        val dockerInfo = replaceDockerInfo(
            dockerInfo = dispatchType.dockerInfo,
            replaceResource = replaceResource
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
        replaceResource: Map<String, PipelineDependentResource>
    ): ThirdPartyAgentIDDispatchType {
        val resourceKey = resourceKey(
            resourceType = PipelineDependentResourceType.BUILD_NODE,
            resourceId = dispatchType.displayName
        )
        val targetNodeId = replaceResource[resourceKey]?.resourceId
        val dockerInfo = replaceDockerInfo(
            dockerInfo = dispatchType.dockerInfo,
            replaceResource = replaceResource
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
        replaceResource: Map<String, PipelineDependentResource>
    ): ThirdPartyAgentDockerInfo? {
        val credential = dockerInfo?.credential ?: return dockerInfo
        val credentialId = credential.credentialId?.takeIf { it.isNotBlank() } ?: return dockerInfo
        if (PipelineVarUtil.isVar(credentialId)) {
            return dockerInfo
        }
        val resourceKey = resourceKey(
            resourceType = PipelineDependentResourceType.CREDENTIAL,
            resourceId = credentialId
        )
        val targetCredential = replaceResource[resourceKey] ?: return dockerInfo
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
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): Element {
        return when {
            element is CodeGitWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeSVNWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGitlabWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGithubWebHookTriggerElement && element.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeTGitWebHookTriggerElement &&
                element.data.input.repositoryType == TriggerRepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.data.input.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
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
                    replaceResourceMap = replaceResourceMap
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
                    replaceResourceMap = replaceResourceMap
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
                    replaceResourceMap = replaceResourceMap
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
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): Element {
        return when {
            element is CodeGitElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeSvnElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is CodeGitlabElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(repositoryHashId = targetRepositoryId)
            }

            element is GithubElement && element.repositoryType == RepositoryType.ID -> {
                val targetRepositoryId = getTargetRepositoryId(
                    repositoryHashId = element.repositoryHashId,
                    replaceResourceMap = replaceResourceMap
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
                        replaceResourceMap = replaceResourceMap
                    ) ?: return element
                    element.copy(
                        data = replaceInputField(
                            data = element.data,
                            fieldName = REPOSITORY_HASH_ID,
                            value = targetRepositoryId
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

    private fun isSubPipelineElement(element: Element): Boolean {
        return element is SubPipelineCallElement ||
            (element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE) ||
            (element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE)
    }

    private fun replaceSubPipelineElement(
        projectId: String,
        element: Element,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): Element {
        return when {
            element is SubPipelineCallElement && element.isIdSubPipelineType() -> {
                val targetPipeline = replaceResourceMap[
                    resourceKey(PipelineDependentResourceType.PIPELINE, element.subPipelineId)
                ] ?: return element
                element.copy(subPipelineId = targetPipeline.resourceId)
            }

            element is MarketBuildAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE -> {
                val replacedData = replaceSubPipelineExecData(
                    projectId = projectId,
                    data = element.data,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(data = replacedData)
            }

            element is MarketBuildLessAtomElement && element.getAtomCode() == SUB_PIPELINE_EXEC_ATOM_CODE -> {
                val replacedData = replaceSubPipelineExecData(
                    projectId = projectId,
                    data = element.data,
                    replaceResourceMap = replaceResourceMap
                ) ?: return element
                element.copy(data = replacedData)
            }

            else -> element
        }
    }

    private fun replaceSubPipelineExecData(
        projectId: String,
        data: Map<String, Any>,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): Map<String, Any>? {
        val input = data[INPUT]
        if (input !is Map<*, *>) {
            return null
        }
        val subProjectId = input["projectId"]?.toString()?.ifBlank { projectId } ?: projectId
        val subPipelineTypeStr = input.getOrDefault("subPipelineType", SubPipelineType.ID.name)
        val subPipelineId = input[SUB_PIPELINE_EXEC_ID]?.toString()
        if (subProjectId != projectId ||
            subPipelineTypeStr != SubPipelineType.ID.name ||
            subPipelineId.isNullOrBlank()
        ) {
            return null
        }
        val targetPipeline = replaceResourceMap[
            resourceKey(PipelineDependentResourceType.PIPELINE, subPipelineId)
        ] ?: return null
        return replaceInputField(
            data = data,
            fieldName = SUB_PIPELINE_EXEC_ID,
            value = targetPipeline.resourceId
        )
    }

    private fun replaceInputField(
        data: Map<String, Any>,
        fieldName: String,
        value: String
    ): Map<String, Any> {
        val input = data[INPUT] as? Map<*, *> ?: return data
        val newInput = input.toMutableMap()
        newInput[fieldName] = value
        return data.toMutableMap().apply {
            this[INPUT] = newInput
        }
    }

    private fun getTargetRepositoryId(
        repositoryHashId: String?,
        replaceResourceMap: Map<String, PipelineDependentResource>
    ): String? {
        val sourceRepositoryHashId = repositoryHashId?.takeIf { it.isNotBlank() } ?: return null
        return replaceResourceMap[
            resourceKey(
                resourceType = PipelineDependentResourceType.REPOSITORY,
                resourceId = sourceRepositoryHashId
            )
        ]?.resourceId
    }

    private fun SubPipelineCallElement.isIdSubPipelineType(): Boolean {
        return subPipelineType == null || subPipelineType == SubPipelineType.ID
    }

    private fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType}_$resourceId"
    }

    companion object {
        private const val INPUT = "input"
        private const val REPOSITORY_HASH_ID = "repositoryHashId"
        private const val SUB_PIPELINE_EXEC_ID = "subPip"
        private val REPO_CHECKOUT_ATOM_CODES = setOf(
            "gitCodeRepo", "PullFromGithub", "Gitlab", "atomtgit", "checkout", "svnCodeRepo"
        )
        private const val SUB_PIPELINE_EXEC_ATOM_CODE = "SubPipelineExec"
    }
}
