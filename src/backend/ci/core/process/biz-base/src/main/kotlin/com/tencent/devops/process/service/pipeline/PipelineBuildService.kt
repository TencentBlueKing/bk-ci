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
import com.tencent.devops.common.api.util.EnvUtils
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
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.ModelTaskIdGenerator
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelinePostElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.utils.QualityUtils
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.template.service.TemplateService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.BUILD_NO
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_REMOTE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_UPDATE_USER
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import com.tencent.devops.process.utils.PipelineVarUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildService(
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val pipelineElementService: PipelinePostElementService,
    private val templateService: TemplateService,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val projectCacheService: ProjectCacheService,
    private val simpleRateLimiter: SimpleRateLimiter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildService::class.java)
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
        private const val CONTEXT_PREFIX = "variables."
    }

    fun startPipeline(
        userId: String,
        pipeline: PipelineInfo,
        startType: StartType,
        pipelineParamMap: MutableMap<String, BuildParameters>,
        channelCode: ChannelCode,
        isMobile: Boolean,
        model: Model,
        signPipelineVersion: Int? = null, // 指定的版本
        frequencyLimit: Boolean = true,
        buildNo: Int? = null,
        startValues: Map<String, String>? = null,
        handlePostFlag: Boolean = true
    ): String {

        val pipelineId = pipeline.pipelineId
        var acquire = false
        val projectId = pipeline.projectId
        val pipelineSetting = pipelineRepositoryService.getSetting(projectId, pipelineId)
        val bucketSize = pipelineSetting!!.maxConRunningQueueSize
        val projectVO = projectCacheService.getProject(projectId)
        if (projectVO?.enabled == false) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_START_BUILD_PROJECT_UNENABLE,
                defaultMessage = "Project [${projectVO.englishName}] has disabled",
                params = arrayOf(projectVO.englishName)
            )
        }
        val lockKey = "PipelineRateLimit:$pipelineId"
        try {
            if (frequencyLimit && channelCode !in NO_LIMIT_CHANNEL) {
                acquire = simpleRateLimiter.acquire(
                    bucketSize ?: PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT, lockKey = lockKey
                )
                if (!acquire) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_START_BUILD_FREQUENT_LIMIT,
                        defaultMessage = "Frequency limit: $bucketSize",
                        params = arrayOf(bucketSize.toString())
                    )
                }
            }

            // 如果指定了版本号，则设置指定的版本号
            pipeline.version = signPipelineVersion ?: pipeline.version

            // 只有新构建才需要填充Post插件与质量红线插件
            val isNewBuild = !pipelineParamMap.containsKey(PIPELINE_RETRY_COUNT)
            if (isNewBuild) {
                fillElementWhenNewBuild(
                    model = model,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    startValues = startValues,
                    startParamsMap = pipelineParamMap,
                    handlePostFlag = handlePostFlag
                )
            }
            val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)

            val userName = when (startType) {
                StartType.PIPELINE -> pipelineParamMap[PIPELINE_START_PIPELINE_USER_ID]?.value ?: userId
                StartType.WEB_HOOK -> pipelineParamMap[PIPELINE_START_WEBHOOK_USER_ID]?.value ?: userId
                StartType.REMOTE -> startValues?.get(PIPELINE_START_REMOTE_USER_ID) ?: userId
                else -> userId
            }
            // 维持原样，保证可修改
            pipelineParamMap[PIPELINE_START_USER_ID] = BuildParameters(key = PIPELINE_START_USER_ID, value = userId)
            pipelineParamMap[PIPELINE_START_USER_NAME] = BuildParameters(PIPELINE_START_USER_NAME, value = userName)
            // 流水线名称有可能变
            pipelineParamMap[PIPELINE_NAME] = BuildParameters(
                key = PIPELINE_NAME,
                value = startValues?.get(PIPELINE_NAME) ?: pipeline.pipelineName
            )
            // 项目名称也是可能变化
            pipelineParamMap[PROJECT_NAME_CHINESE] = BuildParameters(
                key = PROJECT_NAME_CHINESE,
                value = projectVO?.projectName ?: "",
                valueType = BuildFormPropertyType.STRING
            )

            // 解析出定义的流水线变量
            val realStartParamKeys = (model.stages[0].containers[0] as TriggerContainer).params.map { it.id }
            val originStartParams = ArrayList<BuildParameters>(realStartParamKeys.size + 4)
            // 将用户定义的变量增加上下文前缀的版本，与原变量相互独立
            val originStartContexts = ArrayList<BuildParameters>(realStartParamKeys.size)
            realStartParamKeys.forEach { key ->
                pipelineParamMap[key]?.let { param ->
                    originStartParams.add(param)
                    val keyWithPrefix = if (key.startsWith(CONTEXT_PREFIX)) {
                        param.key
                    } else {
                        CONTEXT_PREFIX + param.key
                    }
                    originStartContexts.add(param.copy(key = keyWithPrefix))
                }
            }
            pipelineParamMap.putAll(originStartContexts.associateBy { it.key })
            pipelineParamMap[PIPELINE_BUILD_MSG] = BuildParameters(
                key = PIPELINE_BUILD_MSG,
                value = BuildMsgUtils.getBuildMsg(
                    buildMsg = startValues?.get(PIPELINE_BUILD_MSG)
                        ?: pipelineParamMap[PIPELINE_BUILD_MSG]?.value?.toString(),
                    startType = startType,
                    channelCode = channelCode
                ),
                readOnly = true
            )
            pipelineParamMap[PIPELINE_START_TYPE] = BuildParameters(
                key = PIPELINE_START_TYPE, value = startType.name, readOnly = true
            )
            pipelineParamMap[PIPELINE_START_CHANNEL] = BuildParameters(
                key = PIPELINE_START_CHANNEL, value = channelCode.name, readOnly = true
            )
            pipelineParamMap[PIPELINE_START_MOBILE] = BuildParameters(
                key = PIPELINE_START_MOBILE, value = isMobile, readOnly = true
            )
            pipelineParamMap[PIPELINE_CREATE_USER] = BuildParameters(
                key = PIPELINE_CREATE_USER, value = pipeline.creator, readOnly = true
            )
            pipelineParamMap[PIPELINE_UPDATE_USER] = BuildParameters(
                key = PIPELINE_UPDATE_USER, value = pipeline.lastModifyUser, readOnly = true
            )
            pipelineParamMap[PIPELINE_VERSION] = BuildParameters(PIPELINE_VERSION, pipeline.version, readOnly = true)
            pipelineParamMap[PIPELINE_ID] = BuildParameters(PIPELINE_ID, pipeline.pipelineId, readOnly = true)
            pipelineParamMap[PROJECT_NAME] = BuildParameters(PROJECT_NAME, projectId, readOnly = true)

            pipelineParamMap[BUILD_NO]?.let { buildNoParam -> originStartParams.add(buildNoParam) }
            pipelineParamMap[PIPELINE_BUILD_MSG]?.let { buildMsgParam -> originStartParams.add(buildMsgParam) }
            pipelineParamMap[PIPELINE_RETRY_COUNT]?.let { retryCountParam -> originStartParams.add(retryCountParam) }

            // #6987 修复stream的并发执行判断问题 在判断并发时再替换上下文
            setting?.concurrencyGroup?.let {
                val varMap = pipelineParamMap.values.associate { param -> param.key to param.value.toString() }
                setting.concurrencyGroup = EnvUtils.parseEnv(it, PipelineVarUtil.fillContextVarMap(varMap))
                logger.info("[$pipelineId]|Concurrency Group is ${setting.concurrencyGroup}")
            }

            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(
                    pipelineInfo = pipeline,
                    model = model,
                    startType = startType,
                    setting = setting
                )
            )
            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                throw ErrorCodeException(
                    errorCode = interceptResult.status.toString(),
                    defaultMessage = "Pipeline start failed: [${interceptResult.message}]"
                )
            }

            return pipelineRuntimeService.startBuild(
                pipelineInfo = pipeline,
                fullModel = model,
                // #5264 保留启动参数的原始值以及重试中需要用到的字段
                originStartParams = originStartParams,
                pipelineParamMap = pipelineParamMap,
                buildNo = buildNo,
                buildNumRule = pipelineSetting.buildNumRule,
                setting = setting
            )
        } finally {
            if (acquire) {
                simpleRateLimiter.release(lockKey = lockKey)
            }
        }
    }

    private fun fillElementWhenNewBuild(
        model: Model,
        projectId: String,
        pipelineId: String,
        startValues: Map<String, String>? = null,
        startParamsMap: MutableMap<String, BuildParameters>,
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
                    var skip = false
                    if (startValues != null) {
                        // 优化循环
                        val key = SkipElementUtils.getSkipElementVariableName(element.id)
                        if (startValues[key] == "true") {
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
