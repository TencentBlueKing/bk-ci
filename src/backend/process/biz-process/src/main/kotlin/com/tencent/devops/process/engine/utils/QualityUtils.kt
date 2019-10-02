package com.tencent.devops.process.engine.utils

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.quality.QualityGateInElement
import com.tencent.devops.quality.QualityGateOutElement
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

object QualityUtils {

    private val logger = LoggerFactory.getLogger(QualityUtils::class.java)

    fun getMatchRuleList(
        client: Client,
        projectId: String,
        pipelineId: String,
        templateId: String?
    ): List<QualityRuleMatchTask> {
        return try {
            client.get(ServiceQualityRuleResource::class).matchRuleList(
                projectId,
                pipelineId,
                templateId,
                LocalDateTime.now().timestamp()
            ).data ?: listOf()
        } catch (e: Exception) {
            logger.error("quality get match rule list fail: ${e.message}", e)
            return listOf()
        }
    }

    fun getAuditUserList(
        client: Client,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String
    ): Set<String> {
        return try {
            client.get(ServiceQualityRuleResource::class).getAuditUserList(
                projectId,
                pipelineId,
                buildId,
                taskId
            ).data ?: setOf()
        } catch (e: Exception) {
            logger.error("quality get audit user list fail: ${e.message}", e)
            return setOf()
        }
    }

    /**
     * 动态插入原子
     */
    fun fillInOutElement(
        model: Model,
        startParams: Map<String, Any>,
        ruleMatchTaskList: List<Map<String, Any>>
    ): Model {
        val beforeElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "BEFORE" }.map { it["taskId"] as String }
        val afterElementSet =
            ruleMatchTaskList.filter { it["position"] as String == "AFTER" }.map { it["taskId"] as String }
        val elementRuleMap = ruleMatchTaskList.groupBy { it["taskId"] as String }.toMap()

        val stageList = mutableListOf<Stage>()
        // 1、如果只有一个/零个控制点插件，按照目前的逻辑不变
        // 2、如果有超过一个控制点插件，先检查这些控制点插件的别名是否开头包含质量红线ID+“_”，若包含，则对该控制点设置红线。若没有控制点插件包含，则对所有控制点生效。
        with(model) {
            stages.forEach { stage ->

                val containerList = mutableListOf<Container>()

                stage.containers.forEach { container ->
                    val elementList = mutableListOf<Element>()
                    container.elements.forEach { element ->
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        val skip = if (startParams.containsKey(key)) {
                            val skipValue = startParams[key] as String
                            skipValue == "true"
                        } else {
                            false
                        }

                        if (!skip && beforeElementSet.contains(element.getAtomCode())) {
                            val insertElement = getInsertElement(element, elementRuleMap, true)
                            if (insertElement != null) elementList.add(insertElement)
                        }

                        elementList.add(element)

                        if (!skip && afterElementSet.contains(element.getAtomCode())) {
                            val insertElement = getInsertElement(element, elementRuleMap, false)
                            if (insertElement != null) elementList.add(insertElement)
                        }
                    }

                    val finalContainer = when (container) {
                        is VMBuildContainer -> {
                            VMBuildContainer(
                                containerId = container.containerId,
                                id = container.id,
                                name = container.name,
                                elements = elementList,
                                status = container.status,
                                startEpoch = container.startEpoch,
                                systemElapsed = container.systemElapsed,
                                elementElapsed = container.elementElapsed,
                                baseOS = container.baseOS,
                                vmNames = container.vmNames,
                                maxQueueMinutes = container.maxQueueMinutes,
                                maxRunningMinutes = container.maxRunningMinutes,
                                buildEnv = container.buildEnv,
                                customBuildEnv = container.customBuildEnv,
                                thirdPartyAgentId = container.thirdPartyAgentId,
                                thirdPartyAgentEnvId = container.thirdPartyAgentEnvId,
                                thirdPartyWorkspace = container.thirdPartyWorkspace,
                                dockerBuildVersion = container.dockerBuildVersion,
                                tstackAgentId = container.tstackAgentId,
                                canRetry = container.canRetry,
                                enableExternal = container.enableExternal,
                                jobControlOption = container.jobControlOption,
                                mutexGroup = container.mutexGroup,
                                dispatchType = container.dispatchType,
                                showBuildResource = container.showBuildResource
                            )
                        }
                        is NormalContainer -> {
                            NormalContainer(
                                containerId = container.containerId,
                                id = container.id,
                                name = container.name,
                                elements = elementList,
                                status = container.status,
                                startEpoch = container.startEpoch,
                                systemElapsed = container.systemElapsed,
                                elementElapsed = container.elementElapsed,
                                enableSkip = container.enableSkip,
                                conditions = container.conditions,
                                canRetry = container.canRetry,
                                jobControlOption = container.jobControlOption,
                                mutexGroup = container.mutexGroup
                            )
                        }
                        else -> {
                            container
                        }
                    }
                    containerList.add(finalContainer)
                }
                stageList.add(Stage(containerList, stage.id))
            }

            return Model(name, desc, stageList, labels, instanceFromTemplate, pipelineCreator, null, templateId)

        }
    }

    private fun getInsertElement(
        element: Element,
        elementRuleMap: Map<String, List<Map<String, Any>>>,
        isBefore: Boolean
    ): Element? {
        val position = if (isBefore) "BEFORE" else "AFTER"

        // 取出所有规则的gatewayIds
        val gatewayIds = mutableSetOf<String>()
        val elementList = elementRuleMap[element.getAtomCode()]?.filter { it["position"] as String == position }
        elementList?.forEach {
            // 处理包含某些rule没填gateway id的情况
            val itemGatewayIds = it.getValue("gatewayIds") as List<String>
            if (itemGatewayIds.isEmpty()) gatewayIds.add(("")) else gatewayIds.addAll(itemGatewayIds)
        }
        logger.info("elementName: ${element.name}, gatewayIds: $gatewayIds")
        return if (gatewayIds.isEmpty() || gatewayIds.any { element.name.toLowerCase().contains(it.toLowerCase()) }) {
            val id = "T-${UUIDUtil.generate()}"
            if (isBefore) {
                QualityGateInElement("质量红线(准入)", id, null, element.getAtomCode(), element.name)
            } else {
                QualityGateOutElement("质量红线(准出)", id, null, element.getAtomCode(), element.name)
            }
        } else {
            null
        }
    }
}