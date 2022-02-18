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

package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementBaseInfo
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateInElement
import com.tencent.devops.common.pipeline.pojo.element.quality.QualityGateOutElement
import com.tencent.devops.common.pipeline.utils.ParameterUtils
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelinePostElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.utils.QualityUtils
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PROJECT_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_UPDATE_USER
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Suppress("ALL")
@Service
class PipelineBuildService(
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val pipelineElementService: PipelinePostElementService,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val templateService: TemplateService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val simpleRateLimiter: SimpleRateLimiter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildService::class.java)
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
    }

    fun getModel(projectId: String, pipelineId: String, version: Int? = null) =
        pipelineRepositoryService.getModel(projectId, pipelineId, version) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS,
            defaultMessage = "流水线编排不存在"
        )

    fun subPipelineStartup(
        userId: String,
        startType: StartType = StartType.PIPELINE,
        projectId: String,
        parentProjectId: String,
        parentPipelineId: String,
        parentBuildId: String,
        parentTaskId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        parameters: Map<String, Any>,
        isMobile: Boolean = false,
        triggerUser: String? = null
    ): String {

        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在")

        val startEpoch = System.currentTimeMillis()
        try {

            val model =
                getModel(projectId = projectId, pipelineId = pipelineId, version = readyToBuildPipelineInfo.version)

            val triggerContainer = model.stages[0].containers[0] as TriggerContainer
            val inputBuildParam = mutableListOf<BuildParameters>()
            inputBuildParam.add(BuildParameters(key = PIPELINE_START_PIPELINE_USER_ID, value = triggerUser ?: userId))
            inputBuildParam.add(BuildParameters(key = PIPELINE_START_PARENT_PROJECT_ID, value = parentProjectId))
            inputBuildParam.add(BuildParameters(key = PIPELINE_START_PARENT_PIPELINE_ID, value = parentPipelineId))
            inputBuildParam.add(BuildParameters(key = PIPELINE_START_PARENT_BUILD_ID, value = parentBuildId))
            inputBuildParam.add(BuildParameters(key = PIPELINE_START_PARENT_BUILD_TASK_ID, value = parentTaskId))
            parameters.forEach {
                inputBuildParam.add(BuildParameters(key = it.key, value = it.value))
            }

            val defaultParam = mutableListOf<BuildParameters>()
            triggerContainer.params.forEach {
                defaultParam.add(BuildParameters(
                        key = it.id,
                        value = it.defaultValue,
                        valueType = it.type,
                        readOnly = it.readOnly))
            }
            val startParamsWithType = buildParamCompatibilityTransformer.transform(inputBuildParam, defaultParam)

            // 子流水线的调用不受频率限制
            val subBuildId = startPipeline(
                userId = readyToBuildPipelineInfo.lastModifyUser,
                readyToBuildPipelineInfo = readyToBuildPipelineInfo,
                startType = startType,
                startParamsWithType = startParamsWithType,
                channelCode = channelCode,
                isMobile = isMobile,
                model = model,
                frequencyLimit = false
            )
            // 更新父流水线关联子流水线构建id
            pipelineTaskService.updateSubBuildId(
                projectId = parentProjectId,
                buildId = parentBuildId,
                taskId = parentTaskId,
                subBuildId = subBuildId,
                subProjectId = readyToBuildPipelineInfo.projectId
            )
            return subBuildId
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to start sub-pipeline($pipelineId)")
        }
    }

    fun startPipeline(
        userId: String,
        readyToBuildPipelineInfo: PipelineInfo,
        startType: StartType,
        startParamsWithType: List<BuildParameters>,
        channelCode: ChannelCode,
        isMobile: Boolean,
        model: Model,
        signPipelineVersion: Int? = null, // 指定的版本
        frequencyLimit: Boolean = true,
        buildNo: Int? = null,
        startValues: Map<String, String>? = null,
        handlePostFlag: Boolean = true
    ): String {

        val pipelineId = readyToBuildPipelineInfo.pipelineId
        var acquire = false
        val projectId = readyToBuildPipelineInfo.projectId
        val pipelineSetting = pipelineRepositoryService.getSetting(projectId, pipelineId)
        val bucketSize = pipelineSetting!!.maxConRunningQueueSize
        val lockKey = "PipelineRateLimit:$pipelineId"
        try {
            if (frequencyLimit && channelCode !in NO_LIMIT_CHANNEL) {
                acquire = simpleRateLimiter.acquire(bucketSize, lockKey = lockKey)
                if (!acquire) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_START_BUILD_FREQUENT_LIMIT,
                        defaultMessage = "Frequency limit: $bucketSize",
                        params = arrayOf(bucketSize.toString())
                    )
                }
            }

            // 如果指定了版本号，则设置指定的版本号
            readyToBuildPipelineInfo.version = signPipelineVersion ?: readyToBuildPipelineInfo.version

            val startParamsList = startParamsWithType.toMutableList()
            val startParamMap = startParamsList.associate { it.key to it.value }.toMutableMap()
            // 只有新构建才需要填充Post插件与质量红线插件
            if (!startParamMap.containsKey(PIPELINE_RETRY_COUNT)) {
                fillElementWhenNew(
                    model = model,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    startValues = startValues,
                    startParamsList = startParamsList,
                    startParamsMap = startParamMap,
                    handlePostFlag = handlePostFlag
                )
            }

            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(pipelineInfo = readyToBuildPipelineInfo, model = model, startType = startType)
            )
            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = interceptResult.status.toString(),
                    defaultMessage = "Pipeline start failed: [${interceptResult.message}]"
                )
            }

            val userName = when (startType) {
                StartType.PIPELINE -> ParameterUtils.getListValueByKey(
                    list = startParamsList,
                    key = PIPELINE_START_PIPELINE_USER_ID
                )
                StartType.WEB_HOOK -> ParameterUtils.getListValueByKey(
                    list = startParamsList,
                    key = PIPELINE_START_WEBHOOK_USER_ID
                )
                StartType.MANUAL -> userId
                else -> userId
            }
            val buildMsg = BuildMsgUtils.getBuildMsg(
                buildMsg = ParameterUtils.getListValueByKey(
                    list = startParamsWithType,
                    key = PIPELINE_BUILD_MSG
                ), startType = startType, channelCode = channelCode
            )
            // 增加对containsKey(PIPELINE_NAME)的逻辑判断,如果有传值，默认使用。
            val paramsWithType = startParamsList.asSequence().plus(
                BuildParameters(PIPELINE_VERSION, readyToBuildPipelineInfo.version))
                .plus(BuildParameters(PIPELINE_START_USER_ID, userId))
                .plus(BuildParameters(PIPELINE_START_TYPE, startType.name))
                .plus(BuildParameters(PIPELINE_START_CHANNEL, channelCode.name))
                .plus(BuildParameters(PIPELINE_START_MOBILE, isMobile))
                .plus(
                    if (startValues?.containsKey(PIPELINE_NAME) == true) {
                        BuildParameters(PIPELINE_NAME, startValues[PIPELINE_NAME].toString())
                    } else {
                        BuildParameters(PIPELINE_NAME, readyToBuildPipelineInfo.pipelineName)
                    }
                )
                .plus(BuildParameters(PIPELINE_START_USER_NAME, userName ?: userId))
                .plus(BuildParameters(PIPELINE_BUILD_MSG, buildMsg))
                .plus(BuildParameters(PIPELINE_CREATE_USER, readyToBuildPipelineInfo.creator))
                .plus(BuildParameters(PIPELINE_UPDATE_USER, readyToBuildPipelineInfo.lastModifyUser))
                .plus(BuildParameters(PIPELINE_ID, readyToBuildPipelineInfo.pipelineId)).toList()

            val realStartParamKeys = (model.stages[0].containers[0] as TriggerContainer).params.map { it.id }
            val buildId = pipelineRuntimeService.startBuild(
                pipelineInfo = readyToBuildPipelineInfo,
                fullModel = model,
                // #5264 保留启动参数的原始值以及重试中需要用到的字段
                originStartParams = startParamsWithType.filter {
                    realStartParamKeys.contains(it.key) || it.key == BUILD_NO ||
                        it.key == PIPELINE_BUILD_MSG || it.key == PIPELINE_RETRY_COUNT
                },
                startParamsWithType = paramsWithType,
                buildNo = buildNo,
                buildNumRule = pipelineSetting.buildNumRule
            )
            return buildId
        } finally {
            if (acquire) {
                simpleRateLimiter.release(lockKey = lockKey)
            }
        }
    }

    private fun fillElementWhenNew(
        model: Model,
        projectId: String,
        pipelineId: String,
        startValues: Map<String, String>? = null,
        startParamsList: MutableList<BuildParameters>,
        startParamsMap: MutableMap<String, Any>,
        handlePostFlag: Boolean = true
    ) {
        val templateId = if (model.instanceFromTemplate == true) {
            templateService.getTemplateIdByPipeline(projectId, pipelineId)
        } else {
            null
        }
        val ruleMatchList = pipelineBuildQualityService.getMatchRuleList(projectId, pipelineId, templateId)
        val qualityRuleFlag = ruleMatchList.isNotEmpty()
        var beforeElementSet: List<String>? = null
        var afterElementSet: List<String>? = null
        var elementRuleMap: Map<String, List<Map<String, Any>>>? = null
        if (qualityRuleFlag) {
            val triple = pipelineBuildQualityService.generateQualityRuleElement(ruleMatchList)
            beforeElementSet = triple.first
            afterElementSet = triple.second
            elementRuleMap = triple.third
        }
        val qaSet = setOf(QualityGateInElement.classType, QualityGateOutElement.classType)
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
                    if (startValues != null) {
                        // 优化循环
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        if (startValues[key] == "true") {
                            startParamsList.add(
                                element = BuildParameters(
                                    key = key,
                                    value = "true",
                                    valueType = BuildFormPropertyType.TEMPORARY
                                )
                            )
                            startParamsMap[key] = "true"
                            logger.info("[$pipelineId]|${element.id}|${element.name} will be skipped.")
                        }
                    }
                    // 处理质量红线逻辑
                    if (!qualityRuleFlag) {
                        finalElementList.add(element)
                    } else {
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        val skip = startParamsMap[key] == "true"

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
                            elementItemList.add(ElementBaseInfo(
                                elementId = elementId,
                                elementName = element.name,
                                atomCode = atomCode,
                                version = version,
                                elementJobIndex = elementIndex
                            ))
                        }
                    }
                }
                if (handlePostFlag && elementItemList.isNotEmpty()) {
                    // 校验插件是否能正常使用并返回带post属性的插件
                    pipelineElementService.handlePostElements(
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
    }
}
