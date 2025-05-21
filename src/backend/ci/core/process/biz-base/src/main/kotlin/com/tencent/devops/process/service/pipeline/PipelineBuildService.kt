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

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
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
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.PipelineAsCodeService
import com.tencent.devops.process.service.ProjectCacheService
import com.tencent.devops.process.util.BuildMsgUtils
import com.tencent.devops.process.utils.BK_CI_AUTHORIZER
import com.tencent.devops.process.utils.BK_CI_MATERIAL_ID
import com.tencent.devops.process.utils.BK_CI_MATERIAL_NAME
import com.tencent.devops.process.utils.BK_CI_MATERIAL_URL
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_BUILD_URL
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
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
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_STRING_LENGTH_MAX
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
    private val pipelineSettingVersionService: PipelineSettingVersionService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineElementService: PipelineElementService,
    private val projectCacheService: ProjectCacheService,
    private val pipelineUrlBean: PipelineUrlBean,
    private val simpleRateLimiter: SimpleRateLimiter,
    private val buildIdGenerator: BuildIdGenerator,
    private val pipelineAsCodeService: PipelineAsCodeService
) {
    companion object {
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
        private const val CONTEXT_PREFIX = "variables."
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_EXECUTE,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceIds = "#pipeline?.pipelineId",
            instanceNames = "#pipeline?.pipelineName"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#pipeline?.projectId")],
        scopeId = "#pipeline?.projectId",
        content = ActionAuditContent.PIPELINE_EXECUTE_CONTENT
    )
    fun startPipeline(
        userId: String,
        pipeline: PipelineInfo,
        startType: StartType,
        pipelineParamMap: MutableMap<String, BuildParameters>,
        channelCode: ChannelCode,
        isMobile: Boolean,
        resource: PipelineResourceVersion,
        signPipelineVersion: Int? = null, // 指定的版本
        frequencyLimit: Boolean = true,
        buildNo: Int? = null,
        startValues: Map<String, String>? = null,
        handlePostFlag: Boolean = true,
        webHookStartParam: MutableMap<String, BuildParameters> = mutableMapOf(),
        triggerReviewers: List<String>? = null,
        debug: Boolean? = false
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
        // 如果调试出现了没有关联setting的老版本，则使用当前的配置
        val setting = if (debug == true) {
            pipelineRepositoryService.getDraftVersionResource(
                pipeline.projectId, pipeline.pipelineId
            )?.settingVersion?.let {
                pipelineSettingVersionService.getPipelineSetting(
                    userId = userId,
                    projectId = pipeline.projectId,
                    pipelineId = pipeline.pipelineId,
                    detailInfo = null,
                    version = it
                )
            } ?: pipelineRepositoryService.getSetting(pipeline.projectId, pipeline.pipelineId)
        } else {
            // webhook、重试可以指定流水线版本
            pipelineRepositoryService.getSettingByPipelineVersion(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                pipelineVersion = signPipelineVersion
            )
        }
        if (setting?.failIfVariableInvalid == true) {
            failIfVariableInvalid(pipelineParamMap)
        }
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
            val asCodeSettings = pipelineAsCodeService.getPipelineAsCodeSettings(
                projectId = pipeline.projectId,
                asCodeSettings = setting.pipelineAsCodeSettings
            )
            val pipelineDialectType =
                PipelineDialectUtil.getPipelineDialectType(channelCode = channelCode, asCodeSettings = asCodeSettings)

            // 如果指定了版本号，则设置指定的版本号
            pipeline.version = signPipelineVersion ?: pipeline.version
            val originModelStr = JsonUtil.toJson(resource.model, formatted = false)
            // 只有新构建才需要填充Post插件与质量红线插件
            if (!pipelineParamMap.containsKey(PIPELINE_RETRY_BUILD_ID)) {
                pipelineElementService.fillElementWhenNewBuild(
                    model = resource.model,
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
                isMobile = isMobile,
                pipelineAuthorizer = if (pipeline.channelCode == ChannelCode.BS) {
                    pipelineRepositoryService.getPipelineOauthUser(
                        projectId = pipeline.projectId,
                        pipelineId = pipeline.pipelineId
                    )
                } else {
                    null
                },
                pipelineDialectType = pipelineDialectType.name,
                failIfVariableInvalid = setting.failIfVariableInvalid
            )

            val context = StartBuildContext.init(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                buildId = buildId,
                resourceVersion = pipeline.version,
                modelStr = originModelStr,
                pipelineSetting = setting,
                currentBuildNo = buildNo,
                triggerReviewers = triggerReviewers,
                pipelineParamMap = pipelineParamMap,
                webHookStartParam = webHookStartParam,
                // 解析出定义的流水线变量
                realStartParamKeys = resource.model.getTriggerContainer()
                    .params.map { it.id },
                debug = debug ?: false,
                versionName = resource.versionName,
                yamlVersion = resource.yamlVersion
            )

            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(
                    pipelineInfo = pipeline,
                    model = resource.model,
                    startType = startType,
                    buildId = buildId,
                    runLockType = setting.runLockType,
                    waitQueueTimeMinute = setting.waitQueueTimeMinute,
                    maxQueueSize = setting.maxQueueSize,
                    concurrencyGroup = context.concurrencyGroup,
                    concurrencyCancelInProgress = setting.concurrencyCancelInProgress,
                    maxConRunningQueueSize = setting.maxConRunningQueueSize ?: PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX,
                    retry = pipelineParamMap[PIPELINE_RETRY_COUNT] != null
                )
            )
            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                throw ErrorCodeException(
                    errorCode = interceptResult.status.toString(),
                    defaultMessage = "Pipeline start failed: [${interceptResult.message}]"
                )
            }

            return pipelineRuntimeService.startBuild(fullModel = resource.model, context = context)
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
        isMobile: Boolean,
        debug: Boolean? = false,
        pipelineAuthorizer: String? = null,
        pipelineDialectType: String,
        failIfVariableInvalid: Boolean? = false
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
//        val realStartParamKeys = (model.getTriggerContainer()).params.map { it.id }
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

        if (debug != true) pipelineParamMap[PIPELINE_BUILD_MSG] = BuildParameters(
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
        pipelineParamMap[PIPELINE_DIALECT] = BuildParameters(PIPELINE_DIALECT, pipelineDialectType, readOnly = true)
        if (failIfVariableInvalid == true) {
            pipelineParamMap[PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG] = BuildParameters(
                PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG, true, readOnly = true
            )
        }
        // 自定义触发源材料信息
        startValues?.get(BK_CI_MATERIAL_ID)?.let {
            pipelineParamMap[BK_CI_MATERIAL_ID] = BuildParameters(
                key = BK_CI_MATERIAL_ID,
                value = it,
                readOnly = true
            )
        }
        startValues?.get(BK_CI_MATERIAL_NAME)?.let {
            pipelineParamMap[BK_CI_MATERIAL_NAME] = BuildParameters(
                key = BK_CI_MATERIAL_NAME,
                value = it,
                readOnly = true
            )
        }
        startValues?.get(BK_CI_MATERIAL_URL)?.let {
            pipelineParamMap[BK_CI_MATERIAL_URL] = BuildParameters(
                key = BK_CI_MATERIAL_URL,
                value = it,
                readOnly = true
            )
        }
        // 流水线权限代持人
        pipelineAuthorizer?.let {
            pipelineParamMap[BK_CI_AUTHORIZER] = BuildParameters(
                key = BK_CI_AUTHORIZER,
                value = it,
                readOnly = true
            )
        }

        // 链路
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrBlank()) { // 保存链路信息
            pipelineParamMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] =
                BuildParameters(key = TraceTag.TRACE_HEADER_DEVOPS_BIZID, value = bizId)
        }
//        return originStartParams
    }

    fun failIfVariableInvalid(pipelineParamMap: MutableMap<String, BuildParameters>) {
        pipelineParamMap.forEach { (key, value) ->
            if (value.value.toString().length > PIPELINE_VARIABLES_STRING_LENGTH_MAX) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_FAIL_IF_VARIABLE_INVALID,
                    params = arrayOf(key)
                )
            }
        }
    }
}
