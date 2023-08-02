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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementBaseInfo
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.utils.QualityUtils
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.response.QualityRuleMatchTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Suppress("LongParameterList", "LongMethod", "ComplexMethod")
@Service
class PipelineElementService @Autowired constructor(
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val templateService: TemplateService,
    private val pipelinePostElementService: PipelinePostElementService,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineElementService::class.java)
    }

    fun fillElementWhenNewBuild(
        model: Model,
        projectId: String,
        pipelineId: String,
        startValues: Map<String, String>? = null,
        startParamsMap: MutableMap<String, BuildParameters>? = null,
        handlePostFlag: Boolean = true
    ) {
        val watcher = Watcher(id = "fillElementWhenNewBuild#$pipelineId")
        watcher.start("getTemplateIdByPipeline")
        val templateId = if (model.instanceFromTemplate == true) {
            templateService.getTemplateIdByPipeline(projectId, pipelineId)
        } else {
            null
        }
        watcher.start("getMatchRuleList")
        val ruleMatchList = getMatchRuleList(projectId, pipelineId, templateId)
        val qualityRuleFlag = ruleMatchList.isNotEmpty()
        var beforeElementSet: List<String>? = null
        var afterElementSet: List<String>? = null
        var elementRuleMap: Map<String, List<Map<String, Any>>>? = null
        if (qualityRuleFlag) {
            val triple = QualityUtils.generateQualityRuleElement(ruleMatchList)
            beforeElementSet = triple.first
            afterElementSet = triple.second
            elementRuleMap = triple.third
        }
        val qaSet = setOf(QualityGateInElement.classType, QualityGateOutElement.classType)
        watcher.start("fillElement")
        model.stages.forEachIndexed { index, stage ->
            if (index == 0) {
                return@forEachIndexed
            }
            stage.containers.forEach { container ->
                val finalElementList = mutableListOf<Element>()
                val originalElementList = container.elements
                val elementItemList = mutableListOf<ElementBaseInfo>()
                originalElementList.forEachIndexed nextElement@{ elementIndex, element ->
                    // 清空质量红线相关的element
                    if (element.getClassType() in qaSet) {
                        return@nextElement
                    }
                    var skip = false
                    if (startValues != null) {
                        // 优化循环
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        if (startValues[key] == "true" && startParamsMap != null) {
                            startParamsMap[key] = BuildParameters(
                                key = key, value = "true", valueType = BuildFormPropertyType.TEMPORARY
                            )
                            skip = true
                            logger.info("[$pipelineId]|${element.id}|${element.name} will be skipped.")
                        }
                    }
                    // 处理质量红线逻辑
                    if (!qualityRuleFlag) {
                        finalElementList.add(element)
                    } else {
                        if (!skip && beforeElementSet!!.contains(element.getAtomCode())) {
                            val insertElement = QualityUtils.getInsertElement(element, elementRuleMap!!, true)
                            if (insertElement != null) finalElementList.add(insertElement)
                        }

                        finalElementList.add(element)

                        if (!skip && afterElementSet!!.contains(element.getAtomCode())) {
                            val insertElement = QualityUtils.getInsertElement(element, elementRuleMap!!, false)
                            if (insertElement != null) finalElementList.add(insertElement)
                        }
                    }
                    if (handlePostFlag) {
                        // 处理插件post逻辑
                        if (element is MarketBuildAtomElement || element is MarketBuildLessAtomElement) {
                            var version = element.version
                            if (version.isBlank()) {
                                version = "1.*"
                            }
                            val atomCode = element.getAtomCode()
                            var elementId = element.id
                            if (elementId == null) {
                                elementId = modelTaskIdGenerator.getNextId()
                            }
                            elementItemList.add(
                                ElementBaseInfo(
                                    elementId = elementId,
                                    elementName = element.name,
                                    atomCode = atomCode,
                                    version = version,
                                    elementJobIndex = elementIndex
                                )
                            )
                        }
                    }
                }
                if (handlePostFlag && elementItemList.isNotEmpty()) {
                    // 校验插件是否能正常使用并返回带post属性的插件
                    pipelinePostElementService.handlePostElements(
                        projectId = projectId,
                        elementItemList = elementItemList,
                        originalElementList = originalElementList,
                        finalElementList = finalElementList,
                        startValues = startValues,
                        finallyStage = stage.finally
                    )
                }
                if (finalElementList.size > originalElementList.size) {
                    // 最终生成的元素集合比原元素集合数量多，则说明包含post任务
                    container.containPostTaskFlag = true
                }
                container.elements = finalElementList
            }
        }
        watcher.stop()
    }

    fun getMatchRuleList(projectId: String, pipelineId: String, templateId: String?): List<QualityRuleMatchTask> {
        val startTime = System.currentTimeMillis()
        return try {
            client.get(ServiceQualityRuleResource::class).matchRuleList(
                projectId = projectId,
                pipelineId = pipelineId,
                templateId = templateId,
                startTime = LocalDateTime.now().timestamp()
            ).data ?: listOf()
        } catch (ignore: Exception) {
            logger.error("quality get match rule list fail: ${ignore.message}", ignore)
            return listOf()
        } finally {
            LogUtils.costTime("call rule", startTime)
        }
    }
}
