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
import com.tencent.devops.common.pipeline.pojo.setting.RuntimeResource
import com.tencent.devops.common.redis.concurrent.SimpleRateLimiter
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.environment.api.thirdpartyagent.ServiceThirdPartyAgentResource
import com.tencent.devops.process.bean.PipelineUrlBean
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_CATEGORY
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_IMATE_SESSION_ID
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.atom.AtomUtils
import com.tencent.devops.process.engine.cfg.BuildIdGenerator
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineElementService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStartupPermissionExtService
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
import com.tencent.devops.process.utils.NODE_WORKSPACE
import com.tencent.devops.process.utils.PIPELINE_BUILD_DEBUG
import com.tencent.devops.process.utils.PIPELINE_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_BUILD_URL
import com.tencent.devops.process.utils.PIPELINE_CREATE_USER
import com.tencent.devops.process.utils.PIPELINE_DIALECT
import com.tencent.devops.process.utils.PIPELINE_FAIL_IF_VARIABLE_INVALID_FLAG
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_REPLAY_ORIGIN_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
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
import org.slf4j.LoggerFactory
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
    private val pipelineStartupPermissionExtService: PipelineStartupPermissionExtService,
    private val runtimeResourceResolver: RuntimeResourceResolver,
    private val client: Client
) {
    companion object {
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
        private val logger = LoggerFactory.getLogger(PipelineBuildService::class.java)
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
        val envHashId: String? = null,
        val runtimeResource: RuntimeResource? = null,
        // 重新执行时原构建的触发人。局部重试不会带回原构建的启动参数，没有它触发人会回落成操作人
        val originTriggerUser: String? = null
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
        debug: Boolean? = false,
        originTriggerUser: String? = null
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
        // UI 禁用对所有版本生效，YAML 禁用只对本次启动版本生效
        if (pipeline.locked == true ||
            pipelineRepositoryService.isYamlLockedOnRun(
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                setting = setting
            )
        ) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        if (setting!!.failIfVariableInvalid == true) {
            failIfVariableInvalid(pipelineParamMap)
        }
        setting.correctAndPadding()
        val maxConRunningQueueSize = setting.maxConRunningQueueSize!!
        val lockKey = "PipelineRateLimit:${pipeline.pipelineId}"
        try {
            if (frequencyLimit && channelCode !in NO_LIMIT_CHANNEL) {
                acquire = simpleRateLimiter.acquire(bucketSize = maxConRunningQueueSize, lockKey = lockKey)
                if (!acquire) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_START_BUILD_FREQUENT_LIMIT,
                        defaultMessage = "Frequency limit: $maxConRunningQueueSize",
                        params = arrayOf(maxConRunningQueueSize.toString())
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
                envHashId = setting.envHashId,
                // 运行资源只对「运行环境由设置指定」的渠道生效。没有类型、只存了环境的历史设置会被
                // 还原成环境资源，不按渠道收口的话，普通流水线那些存量 envHashId 会让启动多出
                // 「必须指定运行节点」的拦截，把原本能跑的构建挡住
                // 以流水线自身的渠道为准，不用这次请求带的渠道。
                // 重新执行经常不带创作流渠道，按请求渠道判断会把「节点已不在环境内」整段校验跳掉
                runtimeResource = setting.runtimeResource()
                    ?.takeIf { AtomUtils.isRunEnvSpecifiedBySetting(pipeline.channelCode) },
                originTriggerUser = originTriggerUser
            )
            initPipelineParamMap(initContext)

            // 启动前的扩展权限校验（基于 NODE_AGENT_ID 校验节点操作权限），校验不通过会抛出异常阻断启动。
            // 必须放在运行资源解析之后：绑定单台机器时启动方不传节点，节点是上面解析运行资源才得出来的，
            // 放在解析之前会因为拿不到节点而整个跳过校验，手动启动和定时触发就都绕过了节点权限
            pipelineStartupPermissionExtService.checkStartupPermission(
                userId = userId,
                projectId = pipeline.projectId,
                pipelineId = pipeline.pipelineId,
                pipelineParamMap = pipelineParamMap,
                channelCode = channelCode
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
                    .params
                    .map { it.id }
                    .toMutableList()
                    .let {
                        // 运行资源由设置指定的渠道，启动时[NODE_AGENT_ID、NODE_ENV_HASH_ID、NODE_WORKSPACE]为内置变量。
                        // 看流水线自己的渠道：重新执行的请求渠道默认是 BS，按请求渠道判断会把这三个变量
                        // 排出启动参数，下一次重跑就看不到原环境，环境被换掉的拦截会失效
                        if (AtomUtils.isRunEnvSpecifiedBySetting(pipeline.channelCode)) {
                            it.add(NODE_AGENT_ID)
                            it.add(NODE_ENV_HASH_ID)
                            it.add(NODE_WORKSPACE)
                        }
                        it
                    },
                debug = debug ?: false,
                versionName = resource.versionName,
                yamlVersion = resource.yamlVersion,
                draftVersion = resource.draftVersion
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
                    concurrencySubGroup = context.concurrencySubGroup,
                    maxConRunningQueueSize = maxConRunningQueueSize,
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
        initImateSessionParam(ctx)
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
            // 重新执行时优先沿用原构建的触发人：触发人代表构建因何而来，不应被重试的操作人改写。
            // 局部重试（Stage/Job/插件重试）不会带回原构建的启动参数，上面按触发类型取不到值，
            // 没有这个兜底就会落到操作人，webhook 的提交人、定时的授权人等触发来源都会丢失
        } ?: ctx.originTriggerUser ?: ctx.userId

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
        paramMap[CI_CATEGORY] = BuildParameters(
            key = CI_CATEGORY,
            value = if (AtomUtils.isRunEnvSpecifiedBySetting(ctx.pipeline.channelCode)) {
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
        val resource = ctx.runtimeResource
        // 运行资源级工作空间对所有启动方式生效，Job 未指定时由 DispatchTypeBuilder 回落到该值
        resource?.workspace?.takeIf(String::isNotBlank)?.let { workspace ->
            ctx.pipelineParamMap[NODE_WORKSPACE] = BuildParameters(
                key = NODE_WORKSPACE, value = workspace, readOnly = true
            )
        }
        val startedAgentId = (
            ctx.startValues?.get(NODE_AGENT_ID) ?: ctx.pipelineParamMap[NODE_AGENT_ID]?.value?.toString()
            )?.takeIf(String::isNotBlank)
        // 重放和重新执行都会把原构建的环境带过来，此时以它为准：两者都是要复现原来那次执行，
        // 不能用流水线之后改成的运行资源。新触发的事件不带这个值，仍走设置里的环境
        val startedEnvHashId = (
            ctx.startValues?.get(NODE_ENV_HASH_ID) ?: ctx.pipelineParamMap[NODE_ENV_HASH_ID]?.value?.toString()
            )?.takeIf(String::isNotBlank)
        val agentIdValue = when {
            // 没有绑定运行资源：沿用启动方传入的节点
            resource == null -> startedAgentId
            // 绑定环境只圈定了范围，到底在哪台机器上执行由触发方选定；触发方没传就是接入方的问题，直接拒绝启动，
            // 否则会在环境内落到一台与触发语义无关的机器上
            resource.group -> startedAgentId ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_REQUIRED,
                params = arrayOf(resource.resourceName ?: resource.resourceId ?: "")
            )
            // 绑定单台机器：执行对象固定，新发起的构建一律按当前设置换算，不接受启动方用启动参数改派。
            // 唯一的例外是重跑历史构建（重新执行、重放）：那一次跑在哪台机器上是既成事实，
            // 只要那台机器还在就继续用它，不能因为流水线后来改绑了另一台就把历史构建挪过去。
            // 原机器已经不在时直接拒绝，不改跑当前设置里的那台：记录上仍是重跑原来那次，
            // 执行对象却悄悄换了，用户看不出来。查原机器失败同样不回落，避免一次查询抖动把还在的机器换掉
            else -> ctx.reusableOriginAgentId(startedAgentId)
                ?: runtimeResourceResolver.resolveAgentHashId(
                    projectId = ctx.pipeline.projectId,
                    userId = ctx.pipelineAuthorizer ?: ctx.userId,
                    resource = resource
                )?.takeIf(String::isNotBlank)
                ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_UNRESOLVED,
                    params = arrayOf(resource.resourceName ?: resource.resourceId ?: "")
                )
        }
        // 节点可能在上次构建之后被移出环境，重新执行、重放事件都会带着它启动。
        // 这里不看 runtimeResource 是否解析得出来：重新执行读的是快照版本的设置，
        // 本功能上线前保存的版本里没有运行资源字段，整组回放后 resource 为 null，
        // 但启动参数里仍带着原构建认定的环境，那才是该校验的对象。
        // 用原构建的环境校验时，设置里的环境名对不上这个 hashId，不能拿来报错。
        // 绑定单台机器时执行对象由平台解析，不存在「节点属不属于环境」的问题，不参与校验
        val checkEnvHashId = when {
            // 运行环境不由设置指定的渠道，节点和环境都由编排里的 Job 自己声明，不存在「运行资源的环境」。
            // 看流水线自己的渠道：重新执行的请求渠道经常不是创作流，不能据此跳过
            !AtomUtils.isRunEnvSpecifiedBySetting(ctx.pipeline.channelCode) -> null
            resource?.group == false -> null
            else -> startedEnvHashId ?: resource?.envHashId
        }
        agentIdValue?.takeIf(String::isNotBlank)?.let { agentId ->
            checkAgentInEnv(
                ctx = ctx,
                envHashId = checkEnvHashId,
                resourceName = resource?.resourceName?.takeIf { startedEnvHashId == null },
                agentId = agentId
            )
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
            (startedEnvHashId ?: ctx.envHashId)?.takeIf(String::isNotBlank)?.let { envHashId ->
                paramMap[NODE_ENV_HASH_ID] = BuildParameters(
                    key = NODE_ENV_HASH_ID, value = envHashId, readOnly = true
                )
            }
        }
    }

    /**
     * 重跑历史构建时沿用的原机器。
     *
     * 只在重新执行、重放这两种「把某一次重跑一遍」的操作上生效。
     * 重新执行的标记由重试服务写入参数表；重放的标记只由 buildManualStartup 的 replayOriginBuildId 写入，
     * 启动请求里自带的同名变量不算。
     * 普通启动即使在参数里塞了节点也不认，否则绑定单台机器的「执行对象不可改派」就被绕过去了。
     * 不是重跑、或者这次构建没有记下原机器时返回 null，由调用方按当前设置换算。
     * 原机器已被删除或释放时直接拒绝，不改跑当前设置里的那台。
     * 查询异常原样抛出，不当成机器已释放，也不回落到当前设置。
     */
    private fun InitParamContext.reusableOriginAgentId(startedAgentId: String?): String? {
        val replaying = pipelineParamMap[PIPELINE_REPLAY_ORIGIN_BUILD_ID]?.value?.toString()
            ?.isNotBlank() == true
        val retrying = pipelineParamMap[PIPELINE_RETRY_BUILD_ID]?.value?.toString()?.isNotBlank() == true
        if (!replaying && !retrying) return null
        val agentId = startedAgentId?.takeIf(String::isNotBlank) ?: return null
        val stillThere = try {
            client.get(ServiceThirdPartyAgentResource::class)
                .getAgentById(pipeline.projectId, agentId).data != null
        } catch (ignored: Exception) {
            logger.warn("check origin agent failed|${pipeline.projectId}|$agentId", ignored)
            throw ignored
        }
        if (stillThere) return agentId
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_UNRESOLVED,
            params = arrayOf(
                runtimeResourceResolver.getNodeName(pipeline.projectId, pipelineAuthorizer ?: userId, agentId)
                    ?: agentId
            )
        )
    }

    /**
     * 校验运行节点确实属于本次构建要用的那个环境。
     *
     * 节点由触发方选定，环境来自流水线设置或原事件，两者会随时间脱节：节点被移出环境后，
     * 重新执行、重放事件仍会带着这个节点启动，构建就跑在了一台已经不属于该运行资源的机器上。
     *
     * 查得到归属结论就按结果判定：不属于该环境一律拒绝，包括环境已被清空或删除的情况，
     * 这正是「快照里的环境已经不存在了」要拦住的场景。
     *
     * 查询本身失败（环境服务未发布、调用异常）时同样不放行，但报的是「暂时无法确认」而不是「不在环境内」：
     * 放行等于在最该拦的时候失效——定时触发按保存的节点快照启动，节点早被移出环境，构建却仍然成功。
     * 节点归属问不到时没有别的兜底：派发只认传进来的 agent，不会再判它属不属于这个环境。
     * 这类失败可重试，定时触发下一分钟会自己恢复。
     */
    private fun checkAgentInEnv(
        ctx: InitParamContext,
        envHashId: String?,
        resourceName: String?,
        agentId: String
    ) {
        val targetEnvHashId = envHashId?.takeIf(String::isNotBlank) ?: return
        val authUserId = ctx.pipelineAuthorizer ?: ctx.userId
        val stillInEnv = runtimeResourceResolver.agentStillInEnvOrNull(
            projectId = ctx.pipeline.projectId,
            userId = authUserId,
            envHashId = targetEnvHashId,
            agentHashId = agentId
        ) ?: throw ErrorCodeException(
            // 不带节点名和环境名：查这两个名字同样要问环境服务，这会儿正问不到
            errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_ENV_UNKNOWN
        )
        if (stillInEnv) return
        // 报错给人看，要的是环境名称。设置里的名字只在校验的就是设置里那个环境时才可信，
        // 用历史构建带来的环境校验时按 hashId 现查；环境已被删除等查不到名字的情况才退回展示 hashId
        val envName = resourceName ?: runtimeResourceResolver.getEnvName(
            projectId = ctx.pipeline.projectId,
            userId = authUserId,
            envHashId = targetEnvHashId
        )
        // 同一个环境里可能有多台机器，只报环境名看不出是哪台出了问题，必须带上节点名
        val nodeName = runtimeResourceResolver.getNodeName(
            projectId = ctx.pipeline.projectId,
            userId = authUserId,
            agentHashId = agentId
        )
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_RUNTIME_RESOURCE_NODE_NOT_IN_ENV,
            params = arrayOf(nodeName ?: agentId, envName ?: targetEnvHashId)
        )
    }

    /**
     * 初始化 iMate 会话上下文（由触发源 header 透传，构建运行时只读）
     */
    private fun initImateSessionParam(ctx: InitParamContext) {
        ctx.startValues?.get(CI_IMATE_SESSION_ID)?.takeIf(String::isNotBlank)?.let { sessionId ->
            ctx.pipelineParamMap[CI_IMATE_SESSION_ID] = BuildParameters(
                key = CI_IMATE_SESSION_ID, value = sessionId, readOnly = true
            )
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
