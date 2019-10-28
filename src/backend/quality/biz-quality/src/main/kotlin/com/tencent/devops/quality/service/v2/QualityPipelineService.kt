package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.quality.api.v2.pojo.RulePipelineRange
import com.tencent.devops.quality.api.v2.pojo.RuleTemplateRange
import com.tencent.devops.quality.api.v2.pojo.response.RangeExistElement
import com.tencent.devops.quality.util.ElementUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityPipelineService @Autowired constructor(
    private val client: Client,
    private val indicatorService: QualityIndicatorService
) {
    fun userListPipelineRangeDetail(projectId: String, pipelineIds: Set<String>, indicatorIds: Collection<String>, controlPointType: String?): List<RulePipelineRange> {
        val pipelineElementsMap = client.get(ServicePipelineTaskResource::class).list(projectId, pipelineIds).data ?: mapOf()
        val pipelineNameMap = client.get(ServicePipelineResource::class).getPipelineNameByIds(projectId, pipelineIds).data ?: mapOf()
        val indicatorElements = indicatorService.serviceList(indicatorIds.map { HashUtil.decodeIdToLong(it) }).map { it.elementType }

        // 加入控制点的判断
        val checkElements = if (!controlPointType.isNullOrBlank()) {
            indicatorElements.plus(controlPointType!!)
        } else {
            indicatorElements
        }

        // 剔除已删除的流水线
        return pipelineElementsMap.entries.filter { pipelineNameMap.containsKey(it.key) }.map {
            val pipelineElement = it.value.map { it.classType to it.taskParams }
            // 获取原子信息
            val elementResult = getExistAndLackElements(projectId, checkElements, pipelineElement)
            val existElements = elementResult.first
            val lackElements = elementResult.second

            RulePipelineRange(
                    it.key,
                    pipelineNameMap[it.key] ?: "",
                    pipelineElement.size,
                    lackElements.map { ElementUtils.getElementCnName(it, projectId) },
                    existElements
            )
        }
    }

    fun userListTemplateRangeDetail(projectId: String, templateIds: Set<String>, indicatorIds: Collection<String>, controlPointType: String?): List<RuleTemplateRange> {
        val templateMap = if (templateIds.isNotEmpty()) client.get(ServiceTemplateResource::class)
                .listTemplateById(templateIds, null).data?.templates ?: mapOf()
        else mapOf()
        val templateElementsMap = templateMap.map {
            val model = it.value
            val elements = mutableListOf<Element>()
            model.stages.map { it.containers.map { elements.addAll(it.elements) } }
            it.value.templateId to elements
        }.toMap()
        val templateNameMap = templateMap.map { it.value.templateId to it.value.name }.toMap()

        val indicatorElements = indicatorService.serviceList(indicatorIds.map { HashUtil.decodeIdToLong(it) }).map { it.elementType }
        val checkElements = if (!controlPointType.isNullOrBlank()) {
            indicatorElements.plus(controlPointType!!)
        } else {
            indicatorElements
        }

        // 剔除已删除的流水线
        return templateElementsMap.entries.filter { templateNameMap.containsKey(it.key) }.filter { it.key in templateIds }.map {
            val templateId = it.key
            val templateElements = it.value.map { it.getAtomCode() to it.genTaskParams() }
            // 获取原子信息
            val elementResult = getExistAndLackElements(projectId, checkElements, templateElements)
            val existElements = elementResult.first
            val lackElements = elementResult.second
            RuleTemplateRange(
                    templateId,
                    templateNameMap[it.key] ?: "",
                    templateElements.size,
                    lackElements.map { ElementUtils.getElementCnName(it, projectId) },
                    existElements
            )
        }
    }

    private fun getExistAndLackElements(projectId: String, checkElements: List<String>, originElementList: List<Pair<String, Map<String, Any>>>): Pair<List<RangeExistElement>, Set<String>> {
        val originElements = originElementList.map { it.first }
        val existElements = mutableListOf<RangeExistElement>()
        val codeccElement = originElementList.firstOrNull { it.first == LinuxPaasCodeCCScriptElement.classType }
        if (codeccElement != null) {
            val asynchronous = codeccElement.second["asynchronous"] as? Boolean
            val e = RangeExistElement(LinuxPaasCodeCCScriptElement.classType,
                        ElementUtils.getElementCnName(LinuxPaasCodeCCScriptElement.classType, projectId),
                        1,
                        mapOf("asynchronous" to (asynchronous ?: false)))
            existElements.add(e)
        }

        val lackElements = checkElements.minus(originElements).toMutableSet()

        // 找出流水线存在的指标原子，并统计个数
        val indicatorExistElement = checkElements.minus(lackElements) // 流水线对应的指标原子
        originElements.filter { it in indicatorExistElement }.groupBy { it }
                .forEach { classType, tasks ->
                    existElements.add(RangeExistElement(
                            classType,
                            ElementUtils.getElementCnName(classType, projectId),
                            tasks.size
                    ))
                }
        return Pair(existElements, lackElements)
    }
}