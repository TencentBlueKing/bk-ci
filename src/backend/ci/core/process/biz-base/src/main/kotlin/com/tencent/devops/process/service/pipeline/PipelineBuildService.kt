/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.dialect.PipelineDialectUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_CATEGORY
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.enums.PipelineCategory
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
import com.tencent.devops.process.utils.NODE_AGENT_ID
import com.tencent.devops.process.utils.NODE_ENV_HASH_ID
import com.tencent.devops.process.utils.NODE_HASH_ID
import com.tencent.devops.process.utils.NODE_OS
import com.tencent.devops.process.utils.PIPELINE_BUILD_DEBUG
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
import com.tencent.devops.process.utils.PIPELINE_START_TRIGGER_EVENT_USER_ID
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
    private val pipelineAsCodeService: PipelineAsCodeService,
    private val client: Client
) {
    companion object {
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
    }

    /**
     * 初始化流水线参数的上下文，封装 initPipelineParamMap 所需的全部参数
     */
    data class InitParamContext(
        val buildId: String,
        val startType: StartType,
        val pipelineParamMap: MutableMap<String, BuildParameters>,
        val userId: String,
        val startValues: Map<String, String>?,
        val pipeline: PipelineInfo,
        val projectVO: ProjectVO?,
        val channelCode: ChannelCode,
        val isMobile: Boolean,
        val debug: Boolean? = false,
        val pipelineAuthorizer: String? = null,
        val pipelineDialectType: String,
        val failIfVariableInvalid: Boolean? = false,
        val envHashId: String? = null
    )

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
            val initContext = InitParamContext(
                buildId = buildId,
                startType = startType,
                pipelineParamMap = pipelineParamMap,
                userId = userId,
                startValues = startValues,
                pipeline = pipeline,
                projectVO = projectVO,
                channelCode = channelCode,
                isMobile = isMobile,
                debug = debug,
                pipelineAuthorizer = if (channelCode == ChannelCode.BS || channelCode == ChannelCode.CREATIVE_STREAM) {
                    pipelineRepositoryService.getPipelineOauthUser(
                        projectId = pipeline.projectId,
                        pipelineId = pipeline.pipelineId
                    )
                } else {
                    null
                },
                pipelineDialectType = pipelineDialectType.name,
                failIfVariableInvalid = setting.failIfVariableInvalid,
                envHashId = setting.envHashId
            )
            initPipelineParamMap(initContext)

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
                    .params
                    .map { it.id }
                    .toMutableList()
                    .let {
                        // 创作流启动时[NODE_AGENT_ID、NODE_ENV_HASH_ID]为内置变量
                        if (channelCode == ChannelCode.CREATIVE_STREAM) {
                            it.add(NODE_AGENT_ID)
                            it.add(NODE_ENV_HASH_ID)
                        }
                        it
                    },
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
                    retry = pipelineParamMap[PIPELINE_RETRY_COUNT] != null,
                    retryOnRunningBuild = context.retryOnRunningBuild
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

    private fun initPipelineParamMap(ctx: InitParamContext) {
        initUserAndBasicParams(ctx)
        initBuildParams(ctx)
        initMaterialParams(ctx)
        initAuthorizerParam(ctx)
        initNodeAgentParams(ctx)
        initTraceParams(ctx.pipelineParamMap)
    }

    /**
     * 初始化用户信息和流水线基础参数（用户、名称、项目、启动类型、渠道等）
     */
    private fun initUserAndBasicParams(ctx: InitParamContext) {
        val userName = when (ctx.startType) {
            StartType.PIPELINE -> ctx.pipelineParamMap[PIPELINE_START_PIPELINE_USER_ID]?.value
            StartType.WEB_HOOK -> ctx.pipelineParamMap[PIPELINE_START_WEBHOOK_USER_ID]?.value
            StartType.SERVICE -> ctx.pipelineParamMap[PIPELINE_START_SERVICE_USER_ID]?.value
            StartType.MANUAL -> ctx.pipelineParamMap[PIPELINE_START_MANUAL_USER_ID]?.value
            StartType.TIME_TRIGGER -> ctx.pipelineParamMap[PIPELINE_START_TIME_TRIGGER_USER_ID]?.value
            StartType.REMOTE -> ctx.startValues?.get(PIPELINE_START_REMOTE_USER_ID)
            StartType.TRIGGER_EVENT -> ctx.startValues?.get(PIPELINE_START_TRIGGER_EVENT_USER_ID)
        } ?: ctx.userId

        val paramMap = ctx.pipelineParamMap
        // 维持原样，保证可修改
        paramMap[PIPELINE_START_USER_ID] = BuildParameters(key = PIPELINE_START_USER_ID, value = ctx.userId)
        paramMap[PIPELINE_START_USER_NAME] = BuildParameters(key = PIPELINE_START_USER_NAME, value = userName)
        // 流水线名称有可能变
        paramMap[PIPELINE_NAME] = BuildParameters(
            key = PIPELINE_NAME,
            value = ctx.startValues?.get(PIPELINE_NAME) ?: ctx.pipeline.pipelineName
        )
        // 项目名称也是可能变化
        paramMap[PROJECT_NAME_CHINESE] = BuildParameters(
            key = PROJECT_NAME_CHINESE,
            value = ctx.projectVO?.projectName ?: "",
            valueType = BuildFormPropertyType.STRING
        )
        // 流水线类型
        pipelineParamMap[CI_CATEGORY] = BuildParameters(
            key = CI_CATEGORY,
            value = if (ctx.channelCode == ChannelCode.CREATIVE_STREAM) {
                PipelineCategory.CREATIVE_STREAM
            } else {
                PipelineCategory.PIPELINE
            },
            valueType = BuildFormPropertyType.STRING,
            readOnly = true
        )
        paramMap[PIPELINE_BUILD_MSG] = BuildParameters(
            key = PIPELINE_BUILD_MSG,
            value = BuildMsgUtils.getBuildMsg(
                buildMsg = ctx.startValues?.get(PIPELINE_BUILD_MSG)
                    ?: paramMap[PIPELINE_BUILD_MSG]?.value?.toString(),
                startType = ctx.startType,
                channelCode = ctx.channelCode
            ),
            readOnly = true
        )
        paramMap[PIPELINE_START_TYPE] = BuildParameters(
            key = PIPELINE_START_TYPE, value = ctx.startType.name, readOnly = true
        )
        paramMap[PIPELINE_START_CHANNEL] = BuildParameters(
            key = PIPELINE_START_CHANNEL, value = ctx.channelCode.name, readOnly = true
        )
        paramMap[PIPELINE_START_MOBILE] = BuildParameters(
            key = PIPELINE_START_MOBILE, value = ctx.isMobile, readOnly = true
        )
        paramMap[PIPELINE_CREATE_USER] = BuildParameters(
            key = PIPELINE_CREATE_USER, value = ctx.pipeline.creator, readOnly = true
        )
        paramMap[PIPELINE_UPDATE_USER] = BuildParameters(
            key = PIPELINE_UPDATE_USER, value = ctx.pipeline.lastModifyUser, readOnly = true
        )
        paramMap[PIPELINE_VERSION] = BuildParameters(PIPELINE_VERSION, ctx.pipeline.version, readOnly = true)
        paramMap[PIPELINE_ID] = BuildParameters(PIPELINE_ID, ctx.pipeline.pipelineId, readOnly = true)
        paramMap[PROJECT_NAME] = BuildParameters(PROJECT_NAME, ctx.pipeline.projectId, readOnly = true)
    }

    /**
     * 初始化构建相关参数（构建ID、构建URL、调试标志、语法风格、变量校验开关等）
     */
    private fun initBuildParams(ctx: InitParamContext) {
        val paramMap = ctx.pipelineParamMap
        paramMap[PIPELINE_BUILD_ID] = BuildParameters(PIPELINE_BUILD_ID, ctx.buildId, readOnly = true)
        paramMap[PIPELINE_BUILD_DEBUG] = BuildParameters(
            PIPELINE_BUILD_DEBUG, ctx.debug ?: false, readOnly = true
        )
        paramMap[PIPELINE_BUILD_URL] = BuildParameters(
            key = PIPELINE_BUILD_URL,
            value = pipelineUrlBean.genBuildDetailUrl(
                projectCode = ctx.pipeline.projectId,
                pipelineId = ctx.pipeline.pipelineId,
                buildId = ctx.buildId,
                position = null,
                stageId = null,
                needShortUrl = false
            ),
            readOnly = true
        )
        paramMap[PIPELINE_DIALECT] = BuildParameters(
            PIPELINE_DIALECT, ctx.pipelineDialectType, readOnly = true
        )
        if (ctx.failIfVariableInvalid == true) {
            paramMap[PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG] = BuildParameters(
                PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG, true, readOnly = true
            )
        }
    }

    /**
     * 初始化自定义触发源材料信息
     */
    private fun initMaterialParams(ctx: InitParamContext) {
        val paramMap = ctx.pipelineParamMap
        ctx.startValues?.get(BK_CI_MATERIAL_ID)?.let {
            paramMap[BK_CI_MATERIAL_ID] = BuildParameters(key = BK_CI_MATERIAL_ID, value = it, readOnly = true)
        }
        ctx.startValues?.get(BK_CI_MATERIAL_NAME)?.let {
            paramMap[BK_CI_MATERIAL_NAME] = BuildParameters(key = BK_CI_MATERIAL_NAME, value = it, readOnly = true)
        }
        ctx.startValues?.get(BK_CI_MATERIAL_URL)?.let {
            paramMap[BK_CI_MATERIAL_URL] = BuildParameters(key = BK_CI_MATERIAL_URL, value = it, readOnly = true)
        }
    }

    /**
     * 初始化流水线权限代持人
     */
    private fun initAuthorizerParam(ctx: InitParamContext) {
        ctx.pipelineAuthorizer?.let {
            ctx.pipelineParamMap[BK_CI_AUTHORIZER] = BuildParameters(
                key = BK_CI_AUTHORIZER, value = it, readOnly = true
            )
        }
    }

    /**
     * 初始化节点/Agent 相关参数
     */
    private fun initNodeAgentParams(ctx: InitParamContext) {
        ctx.startValues?.get(NODE_AGENT_ID)?.takeIf(String::isNotBlank)?.let { agentId ->
            val agentInfo = client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(ctx.pipeline.projectId, agentId).data
            val paramMap = ctx.pipelineParamMap
            paramMap[NODE_AGENT_ID] = BuildParameters(key = NODE_AGENT_ID, value = agentId, readOnly = true)
            agentInfo?.os?.takeIf(String::isNotBlank)?.let { os ->
                paramMap[NODE_OS] = BuildParameters(key = NODE_OS, value = os.uppercase(), readOnly = true)
            }
            agentInfo?.nodeId?.takeIf(String::isNotBlank)?.let { nodeId ->
                paramMap[NODE_HASH_ID] = BuildParameters(key = NODE_HASH_ID, value = nodeId, readOnly = true)
            }
            ctx.envHashId?.takeIf(String::isNotBlank)?.let { envHashId ->
                paramMap[NODE_ENV_HASH_ID] = BuildParameters(
                    key = NODE_ENV_HASH_ID, value = envHashId, readOnly = true
                )
            }
        }
    }

    /**
     * 初始化链路追踪参数
     */
    private fun initTraceParams(pipelineParamMap: MutableMap<String, BuildParameters>) {
        val bizId = MDC.get(TraceTag.BIZID)
        if (!bizId.isNullOrBlank()) {
            pipelineParamMap[TraceTag.TRACE_HEADER_DEVOPS_BIZID] =
                BuildParameters(key = TraceTag.TRACE_HEADER_DEVOPS_BIZID, value = bizId)
        }
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
