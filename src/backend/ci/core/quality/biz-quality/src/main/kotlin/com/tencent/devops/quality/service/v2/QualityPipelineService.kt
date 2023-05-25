/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.plugin.codecc.CodeccUtils
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.quality.api.v2.pojo.RulePipelineRange
import com.tencent.devops.quality.api.v2.pojo.RuleTemplateRange
import com.tencent.devops.quality.api.v2.pojo.request.PipelineRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.request.TemplateRangeDetailRequest
import com.tencent.devops.quality.api.v2.pojo.response.RangeExistElement
import com.tencent.devops.quality.util.ElementUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityPipelineService @Autowired constructor(
    private val client: Client,
    private val indicatorService: QualityIndicatorService
) {
    fun userListPipelineRangeDetail(request: PipelineRangeDetailRequest): List<RulePipelineRange> {
        with(request) {
            val pipelineElementsMap = client.get(ServicePipelineTaskResource::class).list(projectId, pipelineIds).data
                ?: mapOf()
            val pipelineNameMap = client.get(ServicePipelineResource::class).getPipelineNameByIds(
                projectId = projectId,
                pipelineIds = pipelineIds
            ).data ?: mapOf()
            val indicatorElements = indicatorService.serviceList(indicatorIds.map { HashUtil.decodeIdToLong(it) })
                .map { it.elementType }

            // 加入控制点的判断
            val checkElements = if (!controlPointType.isNullOrBlank()) {
                indicatorElements.plus(controlPointType!!)
            } else {
                indicatorElements
            }

            // 剔除已删除的流水线
            return pipelineElementsMap.entries.filter { pipelineNameMap.containsKey(it.key) }.map { pipeline ->
                val pipelineElement = pipeline.value.map { CheckElement(it.atomCode, it.taskParams, it.taskName) }
                // 获取原子信息
                val elementResult = getExistAndLackElements(projectId, checkElements, pipelineElement, gatewayId ?: "")
                val existElements = elementResult.first
                val lackElements = elementResult.second

                RulePipelineRange(
                    pipelineId = pipeline.key,
                    pipelineName = pipelineNameMap[pipeline.key] ?: "",
                    elementCount = pipeline.value.size,
                    lackPointElement = lackElements.map { ElementUtils.getElementCnName(it, projectId) },
                    existElement = existElements
                )
            }
        }
    }

    fun userListTemplateRangeDetail(request: TemplateRangeDetailRequest): List<RuleTemplateRange> {
        with(request) {
            val templateMap = if (templateIds.isNotEmpty()) client.get(ServicePTemplateResource::class)
                .listTemplateById(templateIds, projectId, null).data?.templates ?: mapOf()
            else mapOf()
            val templateElementsMap = templateMap.map { template ->
                val model = template.value
                val elements = mutableListOf<Element>()
                model.stages.map { stage -> stage.containers.map { elements.addAll(it.elements) } }
                template.value.templateId to elements
            }.toMap()
            val templateNameMap = templateMap.map { it.value.templateId to it.value.name }.toMap()

            val indicatorElements = indicatorService.serviceList(indicatorIds.map { HashUtil.decodeIdToLong(it) })
                .map { it.elementType }
            val checkElements = if (!controlPointType.isNullOrBlank()) {
                indicatorElements.plus(controlPointType!!)
            } else {
                indicatorElements
            }

            // 剔除已删除的流水线
            return templateElementsMap.entries.filter { templateNameMap.containsKey(it.key) }
                .filter { it.key in templateIds }
                .map { entry ->
                    val templateId = entry.key
                    val templateElements = entry.value.map {
                        CheckElement(it.getAtomCode(), it.genTaskParams(), it.name)
                    }
                    // 获取原子信息
                    val elementResult = getExistAndLackElements(
                        projectId = projectId,
                        checkElements = checkElements,
                        originElementList = templateElements,
                        gatewayId = gatewayId ?: ""
                    )
                    val existElements = elementResult.first
                    val lackElements = elementResult.second
                    RuleTemplateRange(
                        templateId = templateId,
                        templateName = templateNameMap[templateId] ?: "",
                        elementCount = templateElements.size,
                        lackPointElement = lackElements.map { ElementUtils.getElementCnName(it, projectId) },
                        existElement = existElements
                    )
                }
        }
    }

    private fun getExistAndLackElements(
        projectId: String,
        checkElements: List<String>,
        originElementList: List<CheckElement>,
        gatewayId: String
    ): Pair<List<RangeExistElement>, Set<String>> {
        // gateway id过滤一遍
        val matchGatewayOriginElementList = originElementList.filter { it.atomName.contains(gatewayId) }

        // 1. 查找不存在的插件
        val lackElements = mutableSetOf<String>()
        checkElements.forEach { checkElement ->
            val isExist = matchGatewayOriginElementList.any { originElement ->
                getRealAtomCode(checkElement) == getRealAtomCode(originElement.atomCode)
            }
            if (!isExist) {
                // 处理lack element中的codecc插件
                lackElements.add(getRealAtomCode(checkElement))
            }
        }

        // 2. 查找存在的插件
        // 找出流水线存在的指标原子，并统计个数
        val existElements = mutableListOf<RangeExistElement>()
        originElementList.filter { it.atomCode !in lackElements }.groupBy { getRealAtomCode(it.atomCode) }
            .forEach { (classType, tasks) ->
                val ele = if (CodeccUtils.isCodeccAtom(classType)) {
                    val asynchronous = tasks.any { t ->
                        val asynchronous = t.taskParams["asynchronous"] as? Boolean

                        val data = t.taskParams["data"] as? Map<String, Any>
                        val input = data?.get("input") as? Map<String, Any>
                        val newAsync = input?.get("asyncTask") as? Boolean
                        asynchronous == true || newAsync == true
                    }
                    RangeExistElement(
                        name = classType,
                        cnName = ElementUtils.getElementCnName(getRealAtomCode(classType), projectId),
                        count = 1,
                        params = mapOf("asynchronous" to (asynchronous))
                    )
                } else {
                    RangeExistElement(
                        name = classType,
                        cnName = ElementUtils.getElementCnName(classType, projectId),
                        count = tasks.size
                    )
                }
                existElements.add(ele)
            }
        return Pair(existElements, lackElements)
    }

    private fun getRealAtomCode(atomCode: String): String {
        return CodeccUtils.realAtomCodeMap[atomCode] ?: atomCode
    }

    data class CheckElement(
        val atomCode: String,
        val taskParams: Map<String, Any>,
        val atomName: String
    )
}
