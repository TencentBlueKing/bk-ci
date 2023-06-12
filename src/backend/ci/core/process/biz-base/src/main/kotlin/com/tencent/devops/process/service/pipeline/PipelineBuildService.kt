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
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.app.StartBuildContext
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_BUILD_URL
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MANUAL_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_REMOTE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_SERVICE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TIME_TRIGGER_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_UPDATE_USER
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PROJECT_NAME
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class PipelineBuildService(
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineElementService: PipelineElementService,
    private val projectCacheService: ProjectCacheService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val buildIdGenerator: BuildIdGenerator
) {
    companion object {
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
        handlePostFlag: Boolean = true,
        webHookStartParam: MutableMap<String, BuildParameters> = mutableMapOf(),
        triggerReviewers: List<String>? = null
    ): BuildId {

        var acquire = false
        val projectVO = projectCacheService.getProject(pipeline.projectId)
        if (projectVO?.enabled == false) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_START_BUILD_PROJECT_UNENABLE,
                defaultMessage = "Project [${projectVO.englishName}] has disabled",
                params = arrayOf(projectVO.englishName)
            )
        }

        val setting = pipelineRepositoryService.getSetting(pipeline.projectId, pipeline.pipelineId)
        val bucketSize = setting!!.maxConRunningQueueSize
        val lockKey = "PipelineRateLimit:${pipeline.pipelineId}"
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
            if (!pipelineParamMap.containsKey(PIPELINE_RETRY_BUILD_ID)) {
                pipelineElementService.fillElementWhenNewBuild(
                    model = model,
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    startValues = startValues,
                    startParamsMap = pipelineParamMap,
                    handlePostFlag = handlePostFlag
                )
            }

            val buildId = pipelineParamMap[PIPELINE_RETRY_BUILD_ID]?.value?.toString() ?: buildIdGenerator.getNextId()

            initPipelineParamMap(
                buildId = buildId,
                startType = startType,
                pipelineParamMap = pipelineParamMap,
                userId = userId,
                startValues = startValues,
                pipeline = pipeline,
                projectVO = projectVO,
                channelCode = channelCode,
                isMobile = isMobile
            )

            val context = StartBuildContext.init(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildId,
                resourceVersion = pipeline.version,
                pipelineSetting = setting,
                currentBuildNo = buildNo,
                triggerReviewers = triggerReviewers,
                pipelineParamMap = pipelineParamMap,
                webHookStartParam = webHookStartParam,
                // 解析出定义的流水线变量
                realStartParamKeys = (model.stages[0].containers[0] as TriggerContainer).params.map { it.id }
            )

            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(
                    pipelineInfo = pipeline,
                    model = model,
                    startType = startType,
                    buildId = buildId,
                    runLockType = setting.runLockType,
                    waitQueueTimeMinute = setting.waitQueueTimeMinute,
                    maxQueueSize = setting.maxQueueSize,
                    concurrencyGroup = context.concurrencyGroup,
                    concurrencyCancelInProgress = setting.concurrencyCancelInProgress,
                    maxConRunningQueueSize = setting.maxConRunningQueueSize
                )
            )
            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                throw ErrorCodeException(
                    errorCode = interceptResult.status.toString(),
                    defaultMessage = "Pipeline start failed: [${interceptResult.message}]"
                )
            }

            return pipelineRuntimeService.startBuild(fullModel = model, context = context)
        } finally {
            if (acquire) {
                simpleRateLimiter.release(lockKey = lockKey)
            }
        }
    }

    private fun initPipelineParamMap(
        buildId: String,
        startType: StartType,
        pipelineParamMap: MutableMap<String, BuildParameters>,
        userId: String,
        startValues: Map<String, String>?,
        pipeline: PipelineInfo,
        projectVO: ProjectVO?,
        channelCode: ChannelCode,
        isMobile: Boolean
    ) {
        val userName = when (startType) {
            StartType.PIPELINE -> pipelineParamMap[PIPELINE_START_PIPELINE_USER_ID]?.value
            StartType.WEB_HOOK -> pipelineParamMap[PIPELINE_START_WEBHOOK_USER_ID]?.value
            StartType.SERVICE -> pipelineParamMap[PIPELINE_START_SERVICE_USER_ID]?.value
            StartType.MANUAL -> pipelineParamMap[PIPELINE_START_MANUAL_USER_ID]?.value
            StartType.TIME_TRIGGER -> pipelineParamMap[PIPELINE_START_TIME_TRIGGER_USER_ID]?.value
            StartType.REMOTE -> startValues?.get(PIPELINE_START_REMOTE_USER_ID)
        } ?: userId
        // 维持原样，保证可修改
        pipelineParamMap[PIPELINE_START_USER_ID] = BuildParameters(key = PIPELINE_START_USER_ID, value = userId)
        pipelineParamMap[PIPELINE_START_USER_NAME] = BuildParameters(key = PIPELINE_START_USER_NAME, value = userName)
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
//        val realStartParamKeys = (model.stages[0].containers[0] as TriggerContainer).params.map { it.id }
//        val originStartParams = ArrayList<BuildParameters>(realStartParamKeys.size + 4)

        // 将用户定义的变量增加上下文前缀的版本，与原变量相互独立
//        val originStartContexts = ArrayList<BuildParameters>(realStartParamKeys.size)
//        realStartParamKeys.forEach { key ->
//            pipelineParamMap[key]?.let { param ->
//                originStartParams.add(param)
//                originStartContexts.add(
//                    if (key.startsWith(CONTEXT_PREFIX)) {
//                        param
//                    } else {
//                        param.copy(key = CONTEXT_PREFIX + key)
//                    }
//                )
//            }
//        }
//        pipelineParamMap.putAll(originStartContexts.associateBy { it.key })

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
        pipelineParamMap[PROJECT_NAME] = BuildParameters(PROJECT_NAME, pipeline.projectId, readOnly = true)
//
//        pipelineParamMap[BUILD_NO]?.let { buildNoParam -> originStartParams.add(buildNoParam) }
//        pipelineParamMap[PIPELINE_BUILD_MSG]?.let { buildMsgParam -> originStartParams.add(buildMsgParam) }
//        pipelineParamMap[PIPELINE_RETRY_COUNT]?.let { retryCountParam -> originStartParams.add(retryCountParam) }

        pipelineParamMap[PIPELINE_BUILD_ID] = BuildParameters(PIPELINE_BUILD_ID, buildId, readOnly = true)
        pipelineParamMap[PIPELINE_BUILD_URL] = BuildParameters(
            key = PIPELINE_BUILD_URL,
            value = pipelineUrlBean.genBuildDetailUrl(
                projectCode = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildId,
                position = null,
                stageId = null,
                needShortUrl = false
            ),
            readOnly = true
        )

        // 链路
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrBlank()) { // 保存链路信息
            pipelineParamMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] =
                BuildParameters(key = TraceTag.TRACE_HEADER_DEVOPS_BIZID, value = bizId)
        }
//        return originStartParams
    }
}
