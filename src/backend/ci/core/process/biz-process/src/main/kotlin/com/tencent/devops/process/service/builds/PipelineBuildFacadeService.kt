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

package com.tencent.devops.process.service.builds

import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.audit.ActionAuditContent
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.db.pojo.ARCHIVE_SHARDING_DSL_CONTEXT
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildPropertyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.pipeline.utils.CascadePropertyUtils
import com.tencent.devops.common.pipeline.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_HISTORY
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_STATUS
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_VARIABLES
import com.tencent.devops.process.constant.ProcessMessageCode.BK_BUILD_VARIABLES_VALUE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_DETAIL
import com.tencent.devops.process.constant.ProcessMessageCode.BK_USER_NO_PIPELINE_EXECUTE_PERMISSIONS
import com.tencent.devops.process.constant.ProcessMessageCode.BUILD_AGENT_DETAIL_LINK_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO
import com.tencent.devops.process.constant.ProcessMessageCode.USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.compatibility.BuildParametersCompatibilityTransformer
import com.tencent.devops.process.engine.compatibility.BuildPropertyCompatibilityTools
import com.tencent.devops.process.engine.control.lock.BuildIdLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildRunLock
import com.tencent.devops.process.engine.control.lock.PipelineBuildShutdownLock
import com.tencent.devops.process.engine.control.lock.PipelineRefreshBuildLock
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildContainerEvent
import com.tencent.devops.process.engine.service.PipelineBuildDetailService
import com.tencent.devops.process.engine.service.PipelineBuildQualityService
import com.tencent.devops.process.engine.service.PipelineContainerService
import com.tencent.devops.process.engine.service.PipelineRedisService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineStageService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.WebhookBuildParameterService
import com.tencent.devops.process.engine.service.record.ContainerBuildRecordService
import com.tencent.devops.process.engine.service.record.PipelineBuildRecordService
import com.tencent.devops.process.engine.utils.BuildUtils
import com.tencent.devops.process.engine.utils.PipelineUtils
import com.tencent.devops.process.enums.HistorySearchType
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryVariables
import com.tencent.devops.process.pojo.BuildHistoryWithPipelineVersion
import com.tencent.devops.process.pojo.BuildHistoryWithVars
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.ReviewParam
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.process.pojo.VmInfo
import com.tencent.devops.process.pojo.pipeline.BuildRecordInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.pojo.pipeline.PipelineBuildParamFormProp
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.pipeline.StartUpInfo
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.strategy.context.UserPipelinePermissionCheckContext
import com.tencent.devops.process.strategy.factory.UserPipelinePermissionCheckStrategyFactory
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.process.utils.PipelineVarUtil.recommendVersionKey
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 *
 * @version 1.0
 */
@Suppress("ALL")
@Service
class PipelineBuildFacadeService(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineStageService: PipelineStageService,
    private val redisOperation: RedisOperation,
    private val buildDetailService: PipelineBuildDetailService,
    private val buildRecordService: PipelineBuildRecordService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val jmxApi: ProcessJmxApi,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val paramFacadeService: ParamFacadeService,
    private val buildLogPrinter: BuildLogPrinter,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineRedisService: PipelineRedisService,
    private val webhookBuildParameterService: WebhookBuildParameterService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val pipelineBuildRetryService: PipelineBuildRetryService
) {

    @Value("\${pipeline.build.cancel.intervalLimitTime:60}")
    private var cancelIntervalLimitTime: Int = 60 // 取消间隔时间为60秒

    @Value("\${pipeline.build.retry.limit_days:28}")
    private var retryLimitDays: Int = 28

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildFacadeService::class.java)
        private const val RETRY_THIRD_AGENT_ENV = "RETRY_THIRD_AGENT_ENV"
    }

    private fun filterParams(
        userId: String?,
        projectId: String,
        pipelineId: String,
        params: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        return paramFacadeService.filterParams(userId, projectId, pipelineId, params)
    }

    fun buildManualStartupInfo(
        userId: String?,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        version: Int? = null
    ): BuildManualStartupInfo {

        if (checkPermission) { // 不用校验查看权限，只校验执行权限
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId!!,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId
                    )
                )
            )
        }
        val (pipeline, resource, debug) = pipelineRepositoryService.getBuildTriggerInfo(
            projectId, pipelineId, version
        )
        if (pipeline.locked == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        val triggerContainer = resource.model.getTriggerContainer()

        var canManualStartup = false
        var canElementSkip = false
        var useLatestParameters = false
        run lit@{
            triggerContainer.elements.forEach {
                if (it is ManualTriggerElement && it.elementEnabled()) {
                    canManualStartup = true
                    canElementSkip = it.canElementSkip ?: false
                    useLatestParameters = it.useLatestParameters ?: false
                    return@lit
                }
            }
        }
        if (debug) canManualStartup = true

        // 获取最后一次的构建id
        val lastTimeInfo = pipelineRuntimeService.getLastTimeBuild(projectId, pipelineId, debug)
        if (lastTimeInfo?.buildParameters?.isNotEmpty() == true) {
            val latestParamsMap = lastTimeInfo.buildParameters!!.associateBy { it.key }
            triggerContainer.params.forEach { param ->
                val latestParam = latestParamsMap[param.id]
                // 入参、推荐版本号参数有上一次的构建参数的时候才设置成默认值，否者依然使用默认值
                // 当值是boolean类型的时候，需要转为boolean类型
                param.value = when {
                    param.constant == true -> {
                        param.readOnly = true
                        param.defaultValue
                    }

                    !param.required && !recommendVersionKey(param.id) -> {
                        param.defaultValue
                    }

                    param.defaultValue is Boolean -> {
                        latestParam?.value?.toString()?.toBoolean()
                    }

                    else -> {
                        latestParam?.value
                    }
                } ?: param.defaultValue
                // 如果上次构建指定了最新的目录随机字符串，则填充到构建预览信息
                param.latestRandomStringInPath =
                    latestParam?.latestRandomStringInPath ?: param.randomStringInPath
            }
        } else {
            triggerContainer.params.forEach { param ->
                // 如果没有上次构建的记录则直接使用默认值
                param.value = param.defaultValue
                param.latestRandomStringInPath = param.randomStringInPath
            }
        }
        // 构建参数
        val params = getBuildManualParams(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            triggerParams = triggerContainer.params,
            checkPermission = false, // 已校验权限
            debug = debug
        )

        val currentBuildNo = triggerContainer.buildNo?.apply {
            currentBuildNo = pipelineRepositoryService.getBuildNo(
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: buildNo
        }

        return BuildManualStartupInfo(
            canManualStartup = canManualStartup,
            canElementSkip = canElementSkip,
            properties = params,
            buildNo = currentBuildNo,
            useLatestParameters = useLatestParameters
        )
    }

    fun buildManualSearchOptions(
        userId: String?,
        projectId: String,
        pipelineId: String,
        search: String? = null,
        property: BuildFormProperty
    ): List<BuildFormValue> {
        return paramFacadeService.filterOptions(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            search = search,
            property = property
        )
    }

    fun getBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        archiveFlag: Boolean? = false
    ): List<BuildParameters> {
        val userPipelinePermissionCheckStrategy =
            UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
        UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW
        )
        val queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        return pipelineRuntimeService.getBuildParametersFromStartup(projectId, buildId, queryDslContext)
    }

    fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String, // 要重试的构建ID
        taskId: String? = null, // 要重试或跳过的插件ID，或者StageId, 或则stepId
        failedContainer: Boolean? = false, // 仅重试所有失败Job
        skipFailedTask: Boolean? = false, // 跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）
        isMobile: Boolean = false,
        channelCode: ChannelCode? = ChannelCode.BS,
        checkPermission: Boolean? = true,
        checkManualStartup: Boolean? = false
    ): BuildId {
        return pipelineBuildRetryService.retry(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            taskId = taskId,
            failedContainer = failedContainer,
            skipFailedTask = skipFailedTask,
            isMobile = isMobile,
            channelCode = channelCode,
            checkPermission = checkPermission
        )
    }

    fun buildManualStartup(
        userId: String,
        startType: StartType,
        projectId: String,
        pipelineId: String,
        values: Map<String, String>,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        isMobile: Boolean = false,
        startByMessage: String? = null,
        buildNo: Int? = null,
        frequencyLimit: Boolean = true,
        triggerReviewers: List<String>? = null,
        version: Int? = null
    ): BuildId {
        logger.info("[$pipelineId] Manual build start with buildNo[$buildNo] and vars: $values")
        if (checkPermission) {
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }

        val startEpoch = System.currentTimeMillis()
        try {
            val (readyToBuildPipelineInfo, resource, debug) = pipelineRepositoryService.getBuildTriggerInfo(
                projectId, pipelineId, version
            )
            if (readyToBuildPipelineInfo.locked == true) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
            }
            // 正式版本,必须使用最新版本执行
            if (version != null && resource.status == VersionStatus.RELEASED && resource.version != version) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_NON_LATEST_RELEASE_VERSION)
            }

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = resource.model.getTriggerContainer()

            if (startType == StartType.MANUAL && !debug) {
                // 不能通过pipelineInfo里面的canManualStartup来判断,pipelineInfo总是使用的最新版本,但是执行可以使用分支版本
                var canManualStartup = false
                run lit@{
                    triggerContainer.elements.forEach {
                        if (it is ManualTriggerElement && it.elementEnabled()) {
                            canManualStartup = true
                            return@lit
                        }
                    }
                }

                if (!canManualStartup) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.DENY_START_BY_MANUAL
                    )
                }
            } else if (startType == StartType.REMOTE) {
                var canRemoteStartup = false
                run lit@{
                    triggerContainer.elements.forEach {
                        if (it is RemoteTriggerElement && it.elementEnabled()) {
                            canRemoteStartup = true
                            return@lit
                        }
                    }
                }

                if (!canRemoteStartup) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.DENY_START_BY_REMOTE
                    )
                }
            }

            if (buildNo != null) {
                pipelineRuntimeService.updateBuildNo(
                    projectId, pipelineId, buildNo, debug
                )
                logger.info("[$pipelineId] buildNo was changed to [$buildNo]")
            }

            val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(
                userId = userId, projectId = projectId, pipelineId = pipelineId,
                paramProperties = triggerContainer.params, paramValues = values
            )
            // 如果是PAC流水线,需要加上代码库hashId,给checkout:self使用
            pipelineYamlFacadeService.buildYamlManualParamMap(
                projectId = projectId,
                pipelineId = pipelineId
            )?.let {
                paramMap.putAll(it)
            }

            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = readyToBuildPipelineInfo,
                startType = startType,
                pipelineParamMap = paramMap,
                channelCode = channelCode,
                isMobile = isMobile,
                resource = resource,
                frequencyLimit = frequencyLimit,
                buildNo = buildNo,
                startValues = values,
                triggerReviewers = triggerReviewers,
                signPipelineVersion = version,
                debug = debug
            )
        } finally {
            logger.info("[$pipelineId]|$userId|It take(${System.currentTimeMillis() - startEpoch})ms to start pipeline")
        }
    }

    /**
     * 定时触发
     */
    fun timerTriggerPipelineBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        parameters: Map<String, String> = emptyMap(),
        checkPermission: Boolean = true
    ): String? {

        if (checkPermission) {
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }

        val startEpoch = System.currentTimeMillis()
        try {
            // 定时触发不存在调试的情况
            val (pipeline, resource, _) = pipelineRepositoryService.getBuildTriggerInfo(
                projectId, pipelineId, null
            )
            if (pipeline.locked == true) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
            }
            if (pipeline.latestVersionStatus?.isNotReleased() == true) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
            )

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = resource.model.getTriggerContainer()

            val paramPamp = buildParamCompatibilityTransformer.parseTriggerParam(
                userId = userId, projectId = projectId, pipelineId = pipelineId,
                paramProperties = triggerContainer.params, paramValues = parameters
            )
            parameters.forEach { (key, value) ->
                if (!paramPamp.containsKey(key)) {
                    paramPamp[key] = BuildParameters(key = key, value = value)
                }
            }
            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipeline,
                startType = StartType.TIME_TRIGGER,
                pipelineParamMap = paramPamp,
                channelCode = pipeline.channelCode,
                isMobile = false,
                resource = resource,
                signPipelineVersion = null,
                frequencyLimit = false
            ).id
        } finally {
            logger.info("Timer| It take(${System.currentTimeMillis() - startEpoch})ms to start pipeline($pipelineId)")
        }
    }

    /**
     * 代码库回调钩子触发
     */
    fun webhookTriggerPipelineBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        parameters: Map<String, Any> = emptyMap(),
        checkPermission: Boolean = true,
        startType: StartType = StartType.WEB_HOOK,
        startValues: Map<String, String>? = null,
        userParameters: List<BuildParameters>? = null,
        triggerReviewers: List<String>? = null,
        pipelineResource: PipelineResourceVersion? = null,
        pipelineInfo: PipelineInfo? = null
    ): String? {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = USER_NO_PIPELINE_PERMISSION_UNDER_PROJECT,
                    params = arrayOf(userId, projectId, AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)))
                )
            )
        }

        val startEpoch = System.currentTimeMillis()
        try {
            // 定时触发不存在调试的情况
            val (readyToBuildPipelineInfo, resource, _) = if (pipelineResource != null && pipelineInfo != null) {
                Triple(pipelineInfo, pipelineResource, null)
            } else {
                pipelineRepositoryService.getBuildTriggerInfo(
                    projectId, pipelineId, null
                )
            }
            if (readyToBuildPipelineInfo.locked == true) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
            }
            if (readyToBuildPipelineInfo.latestVersionStatus?.isNotReleased() == true) throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
            )

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = resource.model.getTriggerContainer()

            val pipelineParamMap = mutableMapOf<String, BuildParameters>()
            parameters.forEach { (key, value) ->
                pipelineParamMap[key] = BuildParameters(key, value)
            }

            // 添加用户自定义参数
            userParameters?.forEach { param ->
                pipelineParamMap[param.key] = param
            }

            triggerContainer.params.forEach {
                if (pipelineParamMap.contains(it.id)) {
                    return@forEach
                }
                pipelineParamMap[it.id] = BuildParameters(key = it.id, value = it.defaultValue, readOnly = it.readOnly)
            }
            val buildId = pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = readyToBuildPipelineInfo,
                startType = startType,
                pipelineParamMap = pipelineParamMap,
                channelCode = readyToBuildPipelineInfo.channelCode,
                isMobile = false,
                resource = resource,
                signPipelineVersion = null,
                frequencyLimit = false,
                startValues = startValues,
                triggerReviewers = triggerReviewers
            ).id
            if (buildId.isNotBlank()) {
                webhookBuildParameterService.save(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    buildParameters = pipelineParamMap.values.toList()
                )
                if (parameters[PIPELINE_START_TASK_ID] != null) {
                    buildLogPrinter.addLines(
                        buildId = buildId,
                        logMessages = pipelineParamMap.map {
                            LogMessage(
                                message = "${it.key}=${it.value.value}",
                                timestamp = System.currentTimeMillis(),
                                tag = parameters[PIPELINE_START_TASK_ID]?.toString() ?: ""
                            )
                        }
                    )
                }
            }
            return buildId
        } finally {
            logger.info("Webhook| It take(${System.currentTimeMillis() - startEpoch})ms to start pipeline($pipelineId)")
        }
    }

    fun buildManualShutdown(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        terminateFlag: Boolean? = false
    ) {
        if (checkPermission) {
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId,
                        projectId,
                        permission.getI18n(I18nUtil.getLanguage(userId)),
                        pipelineId
                    )
                )
            )
        }

        buildManualShutdown(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            userId = userId,
            channelCode = channelCode,
            terminateFlag = terminateFlag
        )
    }

    fun buildManualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String?,
        params: ReviewParam,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        stepId: String?
    ) {

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )

        if (buildInfo.pipelineId != pipelineId) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
            )
        }

        val model = buildDetailService.get(projectId, buildId)?.model ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        // 对人工审核提交时的参数做必填和范围校验
        checkManualReviewParam(params = params.params)
        var trueElementId = elementId ?: ""

        model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach { cc ->
                cc.elements.forEach element@{ el ->
                    if (!elementId.isNullOrBlank() && el.id != elementId) return@element
                    if (!stepId.isNullOrBlank() && el.stepId != stepId) return@element
                    if (el is ManualReviewUserTaskElement) {
                        trueElementId = el.id!!
                        // Replace the review user with environment
                        val reviewUser = mutableListOf<String>()
                        el.reviewUsers.forEach { user ->
                            reviewUser.addAll(
                                buildVariableService.replaceTemplate(projectId, buildId, user)
                                    .split(",")
                            )
                        }
                        params.params.forEach {
                            when (it.valueType) {
                                ManualReviewParamType.BOOLEAN, ManualReviewParamType.CHECKBOX -> {
                                    it.value = it.value ?: false
                                }

                                else -> {
                                    it.value = buildVariableService.replaceTemplate(
                                        projectId = projectId,
                                        buildId = buildId,
                                        template = it.value.toString()
                                    )
                                }
                            }
                        }
                        if (!reviewUser.contains(userId)) {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_QUALITY_REVIEWER_NOT_MATCH,
                                params = arrayOf(userId)
                            )
                        }
                    }
                }
            }
        }
        logger.info("[$buildId]|buildManualReview|taskId=$trueElementId|userId=$userId|params=$params")

        pipelineRuntimeService.manualDealReview(
            taskId = trueElementId,
            userId = userId,
            params = params.apply {
                this.projectId = projectId
                this.pipelineId = pipelineId
                this.buildId = buildId
            }
        )
        if (params.status == ManualReviewAction.ABORT) {
            buildRecordService.updateBuildCancelUser(
                projectId = projectId,
                buildId = buildId,
                executeCount = buildInfo.executeCount ?: 1,
                cancelUserId = userId
            )
        }
    }

    fun buildTriggerReview(
        userId: String,
        buildId: String,
        pipelineId: String,
        projectId: String,
        approve: Boolean,
        channelCode: ChannelCode = ChannelCode.BS,
        checkPermission: Boolean = true
    ): Boolean {
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE,
                message = MessageUtil.getMessageByLocale(
                    BK_USER_NO_PIPELINE_EXECUTE_PERMISSIONS,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId)
                )
            )
        }
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        if (!buildInfo.isTriggerReviewing()) {
            throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_TRIGGER_NOT_UNDER_REVIEW,
                params = arrayOf(buildId)
            )
        }
        if (!pipelineRuntimeService.checkTriggerReviewer(userId, buildId, pipelineId, projectId)) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_QUALITY_REVIEWER_NOT_MATCH,
                params = arrayOf(userId)
            )
        }
        if (approve) {
            pipelineRuntimeService.approveTriggerReview(userId = userId, buildInfo = buildInfo)
        } else {
            pipelineRuntimeService.disapproveTriggerReview(
                userId = userId, buildId = buildId, pipelineId = pipelineId,
                projectId = projectId, executeCount = buildInfo.executeCount
            )
        }
        return true
    }

    fun buildManualStartStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        isCancel: Boolean,
        reviewRequest: StageReviewRequest?
    ) {
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(buildId)
            )
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )

        if (buildInfo.pipelineId != pipelineId) {
            logger.warn("[$buildId]|buildManualStartStage error|input=$pipelineId|pipeline=${buildInfo.pipelineId}")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
            )
        }

        val buildStage = pipelineStageService.getStage(projectId, buildId, stageId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_STAGE_EXISTS_BY_ID,
                params = arrayOf(stageId)
            )

        if (buildStage.status.name != BuildStatus.PAUSE.name) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_STAGE_IS_NOT_PAUSED,
            params = arrayOf(stageId)
        )
        val group = buildStage.checkIn?.getReviewGroupById(reviewRequest?.id)
        if (group?.id != buildStage.checkIn?.groupToReview()?.id) {
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_REVIEW_GROUP_NOT_FOUND,
                params = arrayOf(stageId, reviewRequest?.id ?: "Flow 1")
            )
        }

        if (buildStage.checkIn?.reviewerContains(userId) != true) {
            throw ErrorCodeException(
                statusCode = Response.Status.FORBIDDEN.statusCode,
                errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                params = arrayOf(stageId)
            )
        }
        PipelineUtils.checkStageReviewParam(reviewRequest?.reviewParams)

        val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.OPERATE_PIPELINE_FAIL,
                defaultMessage = "pipeline($pipelineId) setting is missing.",
                params = arrayOf("pipeline($pipelineId) setting is missing.")
            )
        val runLock = PipelineBuildRunLock(redisOperation, pipelineId)
        try {
            runLock.lock()
            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(
                    pipelineInfo = pipelineInfo,
                    model = null,
                    startType = StartType.MANUAL,
                    buildId = buildId,
                    runLockType = setting.runLockType,
                    waitQueueTimeMinute = setting.waitQueueTimeMinute,
                    maxQueueSize = setting.maxQueueSize,
                    concurrencyGroup = setting.concurrencyGroup,
                    concurrencyCancelInProgress = setting.concurrencyCancelInProgress,
                    maxConRunningQueueSize = setting.maxConRunningQueueSize ?: PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX
                )
            )

            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                logger.warn("[$pipelineId]|START_PIPELINE_MANUAL|pipeline Startup failed:[${interceptResult.message}]")
                throw ErrorCodeException(
                    errorCode = interceptResult.status.toString(),
                    defaultMessage = "Stage Startup failed![${interceptResult.message}]"
                )
            }
            val success = if (isCancel) {
                pipelineStageService.cancelStage(
                    userId = userId,
                    triggerUserId = buildInfo.triggerUser,
                    pipelineName = pipelineInfo.pipelineName,
                    buildNum = buildInfo.buildNum,
                    buildStage = buildStage,
                    reviewRequest = reviewRequest,
                    debug = buildInfo.debug
                )
            } else {
                pipelineStageService.stageManualStart(
                    userId = userId,
                    buildStage = buildStage,
                    reviewRequest = reviewRequest,
                    debug = buildInfo.debug
                )
            }
            if (!success) throw ErrorCodeException(
                statusCode = Response.Status.BAD_REQUEST.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT,
                params = arrayOf(stageId)
            )
        } finally {
            runLock.unlock()
        }
    }

    fun qualityTriggerStage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        stageId: String,
        qualityRequest: StageQualityRequest
    ) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        if (buildInfo.pipelineId != pipelineId) {
            logger.warn("[$buildId]|qualityTriggerStage error|input=$pipelineId|pipeline=${buildInfo.pipelineId}")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
            )
        }

        val buildStage = pipelineStageService.getStage(projectId, buildId, stageId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_STAGE_EXISTS_BY_ID,
                params = arrayOf(stageId)
            )
        val (check, inOrOut) = when (qualityRequest.position) {
            ControlPointPosition.BEFORE_POSITION -> {
                Pair(buildStage.checkIn, true)
            }

            ControlPointPosition.AFTER_POSITION -> {
                Pair(buildStage.checkOut, false)
            }

            else -> {
                throw ErrorCodeException(
                    statusCode = Response.Status.FORBIDDEN.statusCode,
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_STAGE_POSITION_NOT_FOUND,
                    params = arrayOf(stageId, qualityRequest.position)
                )
            }
        }
        if (check?.status != BuildStatus.QUALITY_CHECK_WAIT.name) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_STAGE_IS_NOT_PAUSED,
            params = arrayOf(stageId)
        )
        pipelineStageService.qualityTriggerStage(
            userId = userId,
            buildStage = buildStage,
            qualityRequest = qualityRequest,
            inOrOut = inOrOut,
            check = check
        )
    }

    fun goToReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String
    ): ReviewParam {

        pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )

        val model = buildDetailService.get(projectId, buildId)?.model ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

        model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach { cc ->
                cc.elements.forEach { el ->
                    if (el is ManualReviewUserTaskElement && el.id == elementId) {
                        val reviewUser = mutableListOf<String>()
                        el.reviewUsers.forEach { user ->
                            reviewUser.addAll(
                                buildVariableService.replaceTemplate(projectId, buildId, user)
                                    .split(",")
                            )
                        }
                        el.params.forEach { param ->
                            when (param.valueType) {
                                ManualReviewParamType.BOOLEAN, ManualReviewParamType.CHECKBOX -> {
                                    param.value = param.value ?: false
                                }

                                else -> {
                                    param.value = buildVariableService.replaceTemplate(
                                        projectId = projectId,
                                        buildId = buildId,
                                        template = param.value.toString()
                                    )
                                }
                            }
                        }
                        el.desc = buildVariableService.replaceTemplate(projectId, buildId, el.desc)
                        if (!reviewUser.contains(userId)) {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_QUALITY_REVIEWER_NOT_MATCH,
                                params = arrayOf(userId)
                            )
                        }
                        val reviewParam = ReviewParam(
                            projectId = projectId,
                            pipelineId = pipelineId,
                            buildId = buildId,
                            reviewUsers = reviewUser,
                            status = null,
                            desc = el.desc,
                            suggest = "",
                            params = el.params
                        )
                        logger.info("reviewParam : $reviewParam")
                        return reviewParam
                    }
                }
            }
        }
        return ReviewParam()
    }

    fun serviceShutdown(projectId: String, pipelineId: String, buildId: String, channelCode: ChannelCode) {
        val redisLock = PipelineBuildShutdownLock(redisOperation, buildId)
        try {
            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)

            if (buildInfo == null) {
                return
            } else {
                val parentBuildId = buildInfo.parentBuildId
                if (parentBuildId != null && parentBuildId != buildId) {
                    if (StartType.PIPELINE.name == buildInfo.trigger) {
                        if (buildInfo.parentTaskId != null) {
                            val superPipeline = pipelineRuntimeService.getBuildInfo(projectId, parentBuildId)
                            if (superPipeline != null) {
                                serviceShutdown(
                                    projectId = projectId,
                                    pipelineId = superPipeline.pipelineId,
                                    buildId = superPipeline.buildId,
                                    channelCode = channelCode
                                )
                            }
                        }
                    }
                }
            }

            try {
                pipelineRuntimeService.cancelBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = buildInfo.startUser,
                    executeCount = buildInfo.executeCount ?: 1,
                    buildStatus = BuildStatus.FAILED
                )
                logger.info("$pipelineId|CANCEL_PIPELINE_BUILD|buildId=$buildId|user=${buildInfo.startUser}")
            } catch (t: Throwable) {
                logger.warn("Fail to shutdown the build($buildId) of pipeline($pipelineId)", t)
            }
        } finally {
            redisLock.unlock()
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getBuildDetail(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): ModelDetail {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_DETAIL))
                )
            )
        }

        return getBuildDetail(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = channelCode
        )
    }

    fun getBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): ModelDetail {
        val newModel = buildDetailService.get(projectId, buildId) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

        if (newModel.pipelineId != pipelineId) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS
            )
        }

        pipelineBuildQualityService.addQualityGateReviewUsers(projectId, pipelineId, buildId, newModel.model)

        return newModel
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getBuildDetailByBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: Int,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        debugVersion: Int?
    ): ModelDetail {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_DETAIL))
            )
        )
        // 如果请求的参数是草稿版本的版本号，则用该版本查询调试记录，否则正常调用普通构建
        val targetDebugVersion = debugVersion?.takeIf {
            val draftVersion = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
            draftVersion?.version == debugVersion
        }
        val buildId = pipelineRuntimeService.getBuildIdByBuildNum(
            projectId, pipelineId, buildNo, targetDebugVersion
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf("buildNo=$buildNo")
        )
        return getBuildDetail(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId, channelCode = channelCode
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getBuildRecordByBuildNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        debugVersion: Int?
    ): ModelRecord {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_DETAIL))
            )
        )
        // 如果请求的参数是草稿版本的版本号，则用该版本查询调试记录，否则正常调用普通构建
        val targetDebugVersion = debugVersion?.takeIf {
            val draftVersion = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
            draftVersion?.version == debugVersion
        }
        val buildId = pipelineRuntimeService.getBuildIdByBuildNum(
            projectId, pipelineId, buildNum, targetDebugVersion
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf("buildNum=$buildNum")
        )
        return getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = null,
            channelCode = channelCode,
            encryptedFlag = !pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        )
    }

    fun getBuildRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        channelCode: ChannelCode,
        archiveFlag: Boolean? = false,
        encryptedFlag: Boolean? = false
    ): ModelRecord {
        val queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            buildId = buildId,
            queryDslContext = queryDslContext
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        if (projectId != buildInfo.projectId || pipelineId != buildInfo.pipelineId) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        }
        return buildRecordService.getBuildRecord(
            buildInfo = buildInfo,
            executeCount = executeCount,
            queryDslContext = queryDslContext,
            encryptedFlag = encryptedFlag
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getBuildRecord(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        archiveFlag: Boolean? = false
    ): ModelRecord {

        if (checkPermission) {
            val userPipelinePermissionCheckStrategy =
                UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
            UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW
            )
        }

        return getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            channelCode = channelCode,
            archiveFlag = archiveFlag,
            encryptedFlag = !pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EDIT
            )
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getBuildRecordInfo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): List<BuildRecordInfo> {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = null
            )
        }

        return buildRecordService.getRecordInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun goToLatestFinishedBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): Response {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_DETAIL))
                )
            )
        }
        val buildId = pipelineRuntimeService.getLatestFinishedBuildId(projectId, pipelineId)
        val apiDomain = HomeHostUtil.innerServerHost()
        val redirectURL = when (buildId) {
            null -> "$apiDomain/console/pipeline/$projectId/$pipelineId/history"
            else -> "$apiDomain/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        }
        val uri = UriBuilder.fromUri(redirectURL).build()
        return Response.temporaryRedirect(uri).build()
    }

    fun getBuildStatusWithVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): BuildHistoryWithVars {
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_STATUS))
                )
            )
        }

        val buildHistory = pipelineRuntimeService.getBuildHistoryById(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        val currentQueuePosition = if (BuildStatus.valueOf(buildHistory.status).isReadyToRun()) {
            getCurrentQueuePosition(buildHistory, projectId, pipelineId)
        } else 0

        val variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        return BuildHistoryWithVars(
            id = buildHistory.id,
            userId = buildHistory.userId,
            trigger = buildHistory.trigger,
            buildNum = buildHistory.buildNum,
            pipelineVersion = buildHistory.pipelineVersion,
            startTime = buildHistory.startTime,
            endTime = buildHistory.endTime,
            status = buildHistory.status,
            stageStatus = buildHistory.stageStatus,
            currentTimestamp = buildHistory.currentTimestamp,
            isMobileStart = buildHistory.isMobileStart,
            material = buildHistory.material,
            queueTime = buildHistory.queueTime,
            currentQueuePosition = currentQueuePosition,
            artifactList = buildHistory.artifactList,
            remark = buildHistory.remark,
            totalTime = buildHistory.totalTime,
            executeTime = buildHistory.executeTime,
            buildParameters = buildHistory.buildParameters,
            webHookType = buildHistory.webHookType,
            startType = buildHistory.startType,
            recommendVersion = buildHistory.recommendVersion,
            variables = variables,
            buildMsg = buildHistory.buildMsg,
            retry = buildHistory.retry,
            errorInfoList = buildHistory.errorInfoList,
            buildNumAlias = buildHistory.buildNumAlias,
            webhookInfo = buildHistory.webhookInfo
        )
    }

    /**
     * 拿排队位置，分两种排队。GROUP_LOCK 排队只算当前并发组、 LOCK排队只算当前流水线。
     */
    private fun getCurrentQueuePosition(
        buildHistory: BuildHistory,
        projectId: String,
        pipelineId: String
    ) = if (!buildHistory.concurrencyGroup.isNullOrBlank()) {
        pipelineRuntimeService.getBuildInfoListByConcurrencyGroup(
            projectId = projectId,
            concurrencyGroup = buildHistory.concurrencyGroup!!,
            status = listOf(BuildStatus.QUEUE, BuildStatus.QUEUE_CACHE)
        ).indexOfFirst { it.second == buildHistory.id } + 1
    } else {
        pipelineRuntimeService.getTotalBuildHistoryCount(
            projectId = projectId,
            pipelineId = pipelineId,
            status = listOf(BuildStatus.QUEUE, BuildStatus.QUEUE_CACHE),
            startTimeEndTime = buildHistory.startTime
        )
    }

    fun getBuildVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        checkPermission: Boolean
    ): Result<BuildHistoryVariables> {
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_VARIABLES))
                )
            )
        }

        val buildHistory = pipelineRuntimeService.getBuildHistoryById(projectId, buildId)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId),
                language = I18nUtil.getLanguage(userId)
            )

        val allVariable = buildVariableService.getAllVariable(projectId, pipelineId, buildId)

        return Result(
            BuildHistoryVariables(
                id = buildHistory.id,
                userId = buildHistory.userId,
                trigger = buildHistory.trigger,
                pipelineName = allVariable[PIPELINE_NAME] ?: "",
                buildNum = buildHistory.buildNum ?: 1,
                pipelineVersion = buildHistory.pipelineVersion,
                status = buildHistory.status,
                startTime = buildHistory.startTime,
                endTime = buildHistory.endTime,
                variables = allVariable
            )
        )
    }

    fun getBuildVarsByNames(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        variableNames: List<String>,
        checkPermission: Boolean
    ): Map<String, String> {
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_VARIABLES_VALUE))
                )
            )
        }

        val allVariable = buildVariableService.getAllVariable(projectId, pipelineId, buildId)

        val varMap = HashMap<String, String>()
        variableNames.forEach {
            varMap[it] = (allVariable[it] ?: "")
        }
        return varMap
    }

    fun getBatchBuildStatus(
        projectId: String,
        buildIdSet: Set<String>,
        channelCode: ChannelCode,
        startBeginTime: String?,
        endBeginTime: String?,
        checkPermission: Boolean
    ): List<BuildHistory> {
        val buildHistories = pipelineRuntimeService.getBuildHistoryByIds(
            buildIds = buildIdSet,
            startBeginTime = startBeginTime,
            endBeginTime = endBeginTime,
            projectId = projectId
        )

        if (buildHistories.isEmpty()) {
            return emptyList()
        }
        return buildHistories
    }

    fun getBuilds(
        userId: String,
        projectId: String,
        pipelineId: String?,
        buildStatus: Set<BuildStatus>?,
        checkPermission: Boolean,
        debugVersion: Int?
    ): List<String> {
        return pipelineRuntimeService.getBuilds(
            projectId, pipelineId, buildStatus, debugVersion
        )
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getHistoryBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        updateTimeDesc: Boolean? = null,
        debugVersion: Int? = null
    ): BuildHistoryPage<BuildHistory> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 50
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimitMaxSize(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 50

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )

        val apiStartEpoch = System.currentTimeMillis()
        try {
            if (checkPermission) {
                pipelinePermissionService.validPipelinePermission(
                    userId = userId!!,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.VIEW,
                    message = MessageUtil.getMessageByLocale(
                        ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                        I18nUtil.getLanguage(userId),
                        arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_HISTORY))
                    )
                )
            }
            // 如果请求的参数是草稿版本的版本号，则用该版本查询调试记录，否则正常调用普通构建
            val targetDebugVersion = debugVersion?.takeIf {
                val draftVersion = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
                draftVersion?.version == debugVersion
            }
            val newTotalCount = pipelineRuntimeService.getPipelineBuildHistoryCount(
                projectId = projectId, pipelineId = pipelineId, debugVersion = targetDebugVersion
            )
            val newHistoryBuilds = pipelineRuntimeService.listPipelineBuildHistory(
                projectId = projectId,
                pipelineId = pipelineId,
                offset = offset,
                limit = limit,
                updateTimeDesc = updateTimeDesc
            )
            val buildHistories = mutableListOf<BuildHistory>()
            buildHistories.addAll(newHistoryBuilds)
            val count = newTotalCount + 0L
            // 获取流水线版本号
            val result = BuildHistoryWithPipelineVersion(
                history = SQLPage(count, buildHistories),
                hasDownloadPermission = !checkPermission || pipelinePermissionService.checkPipelinePermission(
                    userId = userId!!,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.EXECUTE
                ),
                pipelineVersion = pipelineInfo.version
            )
            return BuildHistoryPage(
                page = pageNotNull,
                pageSize = limit,
                count = result.history.count,
                records = result.history.records,
                hasDownloadPermission = result.hasDownloadPermission,
                pipelineVersion = result.pipelineVersion
            )
        } finally {
            jmxApi.execute(ProcessJmxApi.LIST_NEW_BUILDS_DETAIL, System.currentTimeMillis() - apiStartEpoch)
        }
    }

    @ActionAuditRecord(
        actionId = ActionId.PIPELINE_VIEW,
        instance = AuditInstanceRecord(
            resourceType = ResourceTypeId.PIPELINE,
            instanceNames = "#pipelineId",
            instanceIds = "#pipelineId"
        ),
        attributes = [AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#projectId")],
        scopeId = "#projectId",
        content = ActionAuditContent.PIPELINE_VIEW_CONTENT
    )
    fun getHistoryBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        materialAlias: List<String>?,
        materialUrl: String?,
        materialBranch: List<String>?,
        materialCommitId: String?,
        materialCommitMessage: String?,
        status: List<BuildStatus>?,
        trigger: List<StartType>?,
        queueTimeStartTime: Long?,
        queueTimeEndTime: Long?,
        startTimeStartTime: Long?,
        startTimeEndTime: Long?,
        endTimeStartTime: Long?,
        endTimeEndTime: Long?,
        totalTimeMin: Long?,
        totalTimeMax: Long?,
        remark: String?,
        buildNoStart: Int?,
        buildNoEnd: Int?,
        buildMsg: String? = null,
        checkPermission: Boolean = true,
        startUser: List<String>? = null,
        updateTimeDesc: Boolean? = null,
        archiveFlag: Boolean? = false,
        debug: Boolean?,
        triggerAlias: List<String>?,
        triggerBranch: List<String>?,
        triggerUser: List<String>?
    ): BuildHistoryPage<BuildHistory> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 50
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 1000

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS
        val queryDslContext = CommonUtils.getJooqDslContext(archiveFlag, ARCHIVE_SHARDING_DSL_CONTEXT)
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            channelCode = channelCode,
            queryDslContext = queryDslContext
        )
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )

        val apiStartEpoch = System.currentTimeMillis()
        try {
            if (checkPermission) {
                val userPipelinePermissionCheckStrategy =
                    UserPipelinePermissionCheckStrategyFactory.createUserPipelinePermissionCheckStrategy(archiveFlag)
                UserPipelinePermissionCheckContext(userPipelinePermissionCheckStrategy).checkUserPipelinePermission(
                    userId = userId!!,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.VIEW
                )
            }

            val newTotalCount = pipelineRuntimeService.getPipelineBuildHistoryCount(
                projectId = projectId,
                pipelineId = pipelineId,
                materialAlias = materialAlias,
                materialUrl = materialUrl,
                materialBranch = materialBranch,
                materialCommitId = materialCommitId,
                materialCommitMessage = materialCommitMessage,
                status = status,
                trigger = trigger,
                queueTimeStartTime = queueTimeStartTime,
                queueTimeEndTime = queueTimeEndTime,
                startTimeStartTime = startTimeStartTime,
                startTimeEndTime = startTimeEndTime,
                endTimeStartTime = endTimeStartTime,
                endTimeEndTime = endTimeEndTime,
                totalTimeMin = totalTimeMin,
                totalTimeMax = totalTimeMax,
                remark = remark,
                buildNoStart = buildNoStart,
                buildNoEnd = buildNoEnd,
                buildMsg = buildMsg,
                startUser = startUser,
                queryDslContext = queryDslContext,
                debug = debug,
                triggerAlias = triggerAlias,
                triggerBranch = triggerBranch,
                triggerUser = triggerUser
            )

            val newHistoryBuilds = pipelineRuntimeService.listPipelineBuildHistory(
                projectId = projectId,
                pipelineId = pipelineId,
                offset = offset,
                limit = limit,
                materialAlias = materialAlias,
                materialUrl = materialUrl,
                materialBranch = materialBranch,
                materialCommitId = materialCommitId,
                materialCommitMessage = materialCommitMessage,
                status = status,
                trigger = trigger,
                queueTimeStartTime = queueTimeStartTime,
                queueTimeEndTime = queueTimeEndTime,
                startTimeStartTime = startTimeStartTime,
                startTimeEndTime = startTimeEndTime,
                endTimeStartTime = endTimeStartTime,
                endTimeEndTime = endTimeEndTime,
                totalTimeMin = totalTimeMin,
                totalTimeMax = totalTimeMax,
                remark = remark,
                buildNoStart = buildNoStart,
                buildNoEnd = buildNoEnd,
                buildMsg = buildMsg,
                startUser = startUser,
                updateTimeDesc = updateTimeDesc,
                queryDslContext = queryDslContext,
                debug = debug,
                triggerAlias = triggerAlias,
                triggerBranch = triggerBranch,
                triggerUser = triggerUser
            )
            val buildHistories = mutableListOf<BuildHistory>()
            buildHistories.addAll(newHistoryBuilds)
            val count = newTotalCount + 0L
            // 获取流水线版本号
            val result = BuildHistoryWithPipelineVersion(
                history = SQLPage(count, buildHistories),
                hasDownloadPermission = checkPermission || pipelinePermissionService.checkPipelinePermission(
                    userId = userId!!,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    permission = AuthPermission.EXECUTE
                ),
                pipelineVersion = pipelineInfo.version
            )
            return BuildHistoryPage(
                page = pageNotNull,
                pageSize = limit,
                count = result.history.count,
                records = result.history.records,
                hasDownloadPermission = result.hasDownloadPermission,
                pipelineVersion = result.pipelineVersion
            )
        } finally {
            jmxApi.execute(ProcessJmxApi.LIST_NEW_BUILDS_DETAIL, System.currentTimeMillis() - apiStartEpoch)
        }
    }

    fun updateRemark(userId: String, projectId: String, pipelineId: String, buildId: String, remark: String?) {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EXECUTE,
            message = MessageUtil.getMessageByLocale(
                BK_USER_NO_PIPELINE_EXECUTE_PERMISSIONS,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId)
            ) + "(Notes cannot be modified)"
        )
        pipelineRuntimeService.updateBuildRemark(projectId, pipelineId, buildId, remark)
    }

    fun getHistoryConditionStatus(userId: String, projectId: String, pipelineId: String): List<IdValue> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_HISTORY))
            )
        )
        val result = mutableListOf<IdValue>()
        BuildStatusSwitcher.pipelineStatusMaker.statusSet().filter { it.visible }.forEach {
            result.add(IdValue(it.name, it.getI18n(I18nUtil.getLanguage(I18nUtil.getRequestUserId()))))
        }
        return result
    }

    fun getHistoryConditionTrigger(userId: String, projectId: String, pipelineId: String): List<IdValue> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_HISTORY))
            )
        )
        return StartType.getStartTypeMap(I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
    }

    fun getHistoryConditionRepo(
        userId: String,
        projectId: String,
        pipelineId: String,
        debugVersion: Int?,
        search: String?,
        type: HistorySearchType?
    ): List<String> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_HISTORY))
            )
        )
        // 如果请求的参数是草稿版本的版本号，则用该版本查询调试记录，否则正常调用普通构建
        val targetDebugVersion = debugVersion?.takeIf {
            val draftVersion = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
            draftVersion?.version == debugVersion
        }
        return pipelineRuntimeService.getHistoryConditionRepo(
            projectId = projectId,
            pipelineId = pipelineId,
            debugVersion = targetDebugVersion,
            search = search,
            type = type
        )
    }

    fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?,
        debugVersion: Int?,
        search: String?,
        type: HistorySearchType?
    ): List<String> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_BUILD_HISTORY))
            )
        )
        // 如果请求的参数是草稿版本的版本号，则用该版本查询调试记录，否则正常调用普通构建
        val targetDebugVersion = debugVersion?.takeIf {
            val draftVersion = pipelineRepositoryService.getDraftVersionResource(projectId, pipelineId)
            draftVersion?.version == debugVersion
        }
        return pipelineRuntimeService.getHistoryConditionBranch(
            projectId = projectId,
            pipelineId = pipelineId,
            aliasList = alias,
            debugVersion = targetDebugVersion,
            search = search,
            type = type
        )
    }

    fun serviceBuildBasicInfo(projectId: String, buildId: String): BuildBasicInfo {
        val build = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        return BuildBasicInfo(
            buildId = buildId,
            projectId = build.projectId,
            pipelineId = build.pipelineId,
            pipelineVersion = build.version,
            status = build.status
        )
    }

    fun batchServiceBasic(buildIds: Set<String>): Map<String, BuildBasicInfo> {
        val buildBasicInfoMap = pipelineRuntimeService.getBuildBasicInfoByIds(buildIds)
        if (buildBasicInfoMap.isEmpty()) {
            return emptyMap()
        }
        return buildBasicInfoMap
    }

    fun getSingleHistoryBuild(
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        buildId: String?,
        channelCode: ChannelCode
    ): BuildHistory? {
        val statusSet = mutableSetOf<BuildStatus>()
        if (buildNum == -1) {
            BuildStatus.values().forEach { status ->
                if (status.isFinish()) {
                    statusSet.add(status)
                } else if (status.isRunning()) {
                    statusSet.add(status)
                }
            }
        }
        val buildInfo = buildId?.let {
            pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId)
        }
        val buildHistory = pipelineRuntimeService.getBuildHistoryByBuildNum(
            projectId = projectId,
            pipelineId = pipelineId,
            buildNum = buildNum,
            statusSet = statusSet,
            debug = buildInfo?.debug
        )
        logger.info("[$pipelineId]|buildHistory=$buildHistory")
        return buildHistory
    }

    fun getLatestSuccessBuild(
        projectId: String,
        pipelineId: String,
        buildId: String?,
        channelCode: ChannelCode
    ): BuildHistory? {
        val buildInfo = buildId?.let {
            pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId)
        }
        val buildHistory = pipelineRuntimeService.getBuildHistoryByBuildNum(
            projectId = projectId,
            pipelineId = pipelineId,
            buildNum = -1,
            statusSet = setOf(BuildStatus.SUCCEED),
            debug = buildInfo?.debug
        )
        logger.info("[$pipelineId]|buildHistory=$buildHistory")
        return buildHistory
    }

    fun getPipelineResourceVersion(projectId: String, pipelineId: String, version: Int? = null) =
        pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, version)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )

    private fun buildManualShutdown(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        channelCode: ChannelCode,
        terminateFlag: Boolean? = false
    ) {

        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        try {
            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            if (buildInfo == null) {
                logger.warn("The build($buildId) of pipeline($pipelineId) is not exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )
            }

            if (buildInfo.status.isFinish()) {
                logger.warn("The build $buildId of project $projectId already finished ")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.PIPELINE_BUILD_HAS_ENDED_CANNOT_BE_OPERATE
                )
            }

            if (buildInfo.pipelineId != pipelineId) {
                logger.warn("shutdown error: input|$pipelineId| buildId-pipeline| ${buildInfo.pipelineId}| $buildId")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
                )
            }

            val finalTerminateFlag = if (terminateFlag == true) {
                terminateFlag
            } else {
                // 兼容post任务的场景，处于”运行中“的构建可以支持多次取消操作(第二次取消直接强制终止流水线构建)
                val cancelActionTime = redisOperation.get(BuildUtils.getCancelActionBuildKey(buildId))?.toLong() ?: 0
                val intervalTime = System.currentTimeMillis() - cancelActionTime
                var flag = false // 是否强制终止
                if (intervalTime <= cancelIntervalLimitTime * 1000) {
                    val alreadyCancelUser = buildRecordService.getBuildCancelUser(pipelineId, buildId, buildInfo.executeCount)
                    logger.warn("The build $buildId of project $projectId already cancel by user $alreadyCancelUser")
                    val timeTip = cancelIntervalLimitTime - intervalTime / 1000
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.CANCEL_BUILD_BY_OTHER_USER,
                        params = arrayOf(userId, timeTip.toString())
                    )
                } else if (cancelActionTime > 0) {
                    flag = true
                }
                flag
            }

            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)

            if (pipelineInfo == null) {
                logger.warn("The pipeline($pipelineId) of project($projectId) is not exist")
                return
            }
            if (pipelineInfo.channelCode != channelCode) {
                return
            }

            val tasks = pipelineTaskService.getRunningTask(projectId, buildId)

            tasks.forEach { task ->
                val taskId = task["taskId"]?.toString() ?: ""
                val stepId = task["stepId"]?.toString() ?: ""
                val containerId = task["containerId"]?.toString() ?: ""
                val status = task["status"] ?: ""
                val executeCount = task["executeCount"] as? Int ?: 1
                logger.info("build($buildId) shutdown by $userId, taskId: $taskId, status: $status")
                val cancelTaskSetKey = TaskUtils.getCancelTaskIdRedisKey(buildId, containerId, false)
                redisOperation.addSetValue(cancelTaskSetKey, taskId)
                redisOperation.expire(cancelTaskSetKey, TimeUnit.DAYS.toSeconds(Timeout.MAX_JOB_RUN_DAYS))
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "Cancelled by $userId",
                    tag = taskId,
                    containerHashId = containerId,
                    executeCount = executeCount,
                    jobId = null,
                    stepId = stepId
                )
            }

            if (tasks.isEmpty()) {
                val jobId = "0"
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "Cancelled by $userId",
                    tag = VMUtils.genStartVMTaskId(jobId),
                    containerHashId = jobId,
                    executeCount = 1,
                    jobId = null,
                    stepId = VMUtils.genStartVMTaskId(jobId)
                )
            }

            try {
                pipelineRuntimeService.cancelBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = userId,
                    executeCount = buildInfo.executeCount ?: 1,
                    buildStatus = BuildStatus.CANCELED,
                    terminateFlag = finalTerminateFlag
                )
                logger.info("Cancel the pipeline($pipelineId) of instance($buildId) by the user($userId)")
            } catch (t: Throwable) {
                logger.warn("Fail to shutdown the build($buildId) of pipeline($pipelineId)", t)
            }
        } finally {
            redisLock.unlock()
        }
    }

    fun getPipelineLatestBuildByIds(projectId: String, pipelineIds: List<String>): Map<String, PipelineLatestBuild> {
        logger.info("getPipelineLatestBuildByIds: $projectId | $pipelineIds")

        return pipelineRuntimeService.getLatestBuild(projectId, pipelineIds)
    }

    /**
     * @return <启动人，#9910 环境构建时遇到启动错误时调度到一个新的Agent 是否重新调度>
     */
    fun workerBuildFinish(
        projectCode: String,
        pipelineId: String, /* pipelineId在agent请求的数据有值前不可用 */
        buildId: String,
        vmSeqId: String,
        nodeHashId: String?,
        executeCount: Int?,
        simpleResult: SimpleResult
    ): Pair<String?, Boolean> {
        var msg = simpleResult.message

        if (!nodeHashId.isNullOrBlank()) {
            msg = "${
                I18nUtil.getCodeLanMessage(
                    messageCode = BUILD_AGENT_DETAIL_LINK_ERROR,
                    params = arrayOf(projectCode, nodeHashId)
                )
            } $msg"
        }
        // #5046 worker-agent.jar进程意外退出，经由devopsAgent转达
        if (simpleResult.success) {
            val startUpVMTask = pipelineTaskService.getBuildTask(
                projectId = projectCode,
                buildId = buildId,
                taskId = VMUtils.genStartVMTaskId(vmSeqId)
            )
            if (startUpVMTask?.status?.isRunning() == true) {
                msg = "$msg| ${
                    I18nUtil.getCodeLanMessage(messageCode = ProcessMessageCode.BUILD_WORKER_DEAD_ERROR)
                }"
            } else {
                logger.info("[$buildId]|Job#$vmSeqId| worker had been exit. msg=$msg")
                msg?.let { self ->
                    buildLogPrinter.addLine(
                        buildId = buildId,
                        message = self,
                        tag = startUpVMTask!!.taskId,
                        containerHashId = startUpVMTask.containerHashId,
                        executeCount = startUpVMTask.executeCount ?: 1,
                        jobId = null, stepId = startUpVMTask.stepId
                    )
                }
                return Pair(startUpVMTask?.starter, false)
            }
        } else {
            msg = "$msg| ${I18nUtil.getCodeLanMessage(ProcessMessageCode.BUILD_WORKER_DEAD_ERROR)}"
            // #9910 环境构建时遇到启动错误时调度到一个新的Agent
            // 通过更新 task param 一个固定的参数，使其在尝试完成时重新发送启动请求
            if (!simpleResult.ignoreAgentIds.isNullOrEmpty()) {
                val startUpVMTask = pipelineTaskService.getBuildTask(
                    projectId = projectCode,
                    buildId = buildId,
                    taskId = VMUtils.genStartVMTaskId(vmSeqId)
                )
                if (startUpVMTask?.status?.isRunning() == true) {
                    val taskParam = startUpVMTask.taskParams
                    taskParam[RETRY_THIRD_AGENT_ENV] = simpleResult.ignoreAgentIds!!.joinToString(",")
                    pipelineTaskService.updateTaskParam(
                        transactionContext = null,
                        projectId = startUpVMTask.projectId,
                        buildId = startUpVMTask.buildId,
                        taskId = startUpVMTask.taskId,
                        taskParam = JsonUtil.toJson(taskParam)
                    )
                    return Pair(startUpVMTask.starter, true)
                }
            }
        }

        // 添加错误码日志
        val realErrorType = ErrorType.getErrorType(simpleResult.error?.errorType)
        simpleResult.error?.errorType.let {
            msg = "$msg \nerrorType: ${realErrorType?.getI18n(I18nUtil.getDefaultLocaleLanguage())}"
        }
        simpleResult.error?.errorCode.let { msg = "$msg \nerrorCode: ${simpleResult.error?.errorCode}" }
        simpleResult.error?.errorMessage.let { msg = "$msg \nerrorMsg: ${simpleResult.error?.errorMessage}" }

        val buildInfo = pipelineRuntimeService.getBuildInfo(projectCode, buildId)
        if (buildInfo == null || buildInfo.status.isFinish()) {
            logger.warn("[$buildId]|workerBuildFinish|The build status is ${buildInfo?.status}")
            return Pair(buildInfo?.startUser, false)
        }

        if (executeCount != null && buildInfo.executeCount != null && executeCount != buildInfo.executeCount) {
            logger.warn("[$buildId]|workerBuildFinish|executeCount ne [$executeCount != ${buildInfo.executeCount}]")
            return Pair(buildInfo.startUser, false)
        }

        val container = pipelineContainerService.getContainer(
            projectId = buildInfo.projectId,
            buildId = buildId,
            stageId = null,
            containerId = vmSeqId
        )
        if (container != null) {
            val stage = pipelineStageService.getStage(
                projectId = buildInfo.projectId,
                buildId = buildId,
                stageId = container.stageId
            )
            if (stage != null && stage.status.isRunning()) { // Stage 未处于运行中，不接受下面容器结束事件
                logger.info("[$buildId]|Job#$vmSeqId|${simpleResult.success}|$msg")
                pipelineEventDispatcher.dispatch(
                    PipelineBuildContainerEvent(
                        source = "worker_build_finish",
                        projectId = buildInfo.projectId,
                        pipelineId = buildInfo.pipelineId,
                        userId = buildInfo.startUser,
                        buildId = buildId,
                        stageId = container.stageId,
                        containerId = vmSeqId,
                        containerHashId = container.containerHashId,
                        containerType = container.containerType,
                        executeCount = container.executeCount,
                        actionType = ActionType.TERMINATE,
                        reason = msg,
                        errorCode = simpleResult.error?.errorCode ?: 0,
                        errorTypeName = realErrorType?.getI18n(I18nUtil.getDefaultLocaleLanguage())
                    )
                )
            }
        }

        return Pair(buildInfo.startUser, false)
    }

    fun saveBuildVmInfo(projectId: String, pipelineId: String, buildId: String, vmSeqId: String, vmInfo: VmInfo) {
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        containerBuildRecordService.saveBuildVmInfo(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            containerId = vmSeqId,
            vmInfo = vmInfo,
            executeCount = buildInfo.executeCount
        )
    }

    fun getBuildDetailStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): String {
        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = MessageUtil.getMessageByLocale(
                    ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, pipelineId, I18nUtil.getCodeLanMessage(BK_DETAIL))
                )
            )
        }
        return pipelineRuntimeService.getBuildInfo(projectId, buildId)?.status?.name
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
    }

    fun replayBuild(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        forceTrigger: Boolean
    ): BuildId {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EXECUTE,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    AuthPermission.EXECUTE.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        val buildInfo = checkPipelineInfo(projectId, pipelineId, buildId)
        val buildVars = buildVariableService.getAllVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            keys = setOf(PIPELINE_START_TASK_ID)
        )
        // 按原有的启动参数组装启动参数(排除重试次数)
        val startParameters = buildInfo.buildParameters?.filter {
            it.key != PIPELINE_RETRY_COUNT
        }?.associate {
            it.key to if (CascadePropertyUtils.supportCascadeParam(it.valueType) && it.value is Map<*, *>) {
                JsonUtil.toJson(it.value)
            } else {
                it.value.toString()
            }
        }?.toMutableMap() ?: mutableMapOf()
        startParameters.putAll(buildVars)
        val startType = StartType.toStartType(buildInfo.trigger)
        // 定时触发不存在调试的情况
        val (readyToBuildPipelineInfo, resource, _) = pipelineRepositoryService.getBuildTriggerInfo(
            projectId, pipelineId, null
        )
        if (readyToBuildPipelineInfo.locked == true) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK)
        }
        if (readyToBuildPipelineInfo.latestVersionStatus?.isNotReleased() == true) throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
        )
        val model = resource.model
        val triggerContainer = model.getTriggerContainer()
        // 检查触发器是否存在
        val checkTriggerResult = forceTrigger || when (startType) {
            StartType.WEB_HOOK -> {
                triggerContainer.elements.find { it.id == startParameters[PIPELINE_START_TASK_ID] }
            }

            StartType.MANUAL, StartType.SERVICE -> {
                triggerContainer.elements.find { it is ManualTriggerElement }
            }

            StartType.REMOTE -> {
                triggerContainer.elements.find { it is RemoteTriggerElement }
            }

            StartType.TIME_TRIGGER -> {
                triggerContainer.elements.find { it.id == startParameters[PIPELINE_START_TASK_ID] }
            }

            StartType.PIPELINE -> {
                EmptyElement()
            }
        } != null
        if (!checkTriggerResult) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TRIGGER_CONDITION_NOT_MATCH,
                params = arrayOf(resource.versionName ?: "")
            )
        }
        return triggerPipeline(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            startParameters = startParameters,
            startType = if (startType == StartType.WEB_HOOK) {
                StartType.WEB_HOOK
            } else {
                StartType.MANUAL
            },
            pipelineInfo = readyToBuildPipelineInfo,
            pipelineResourceVersion = resource
        )
    }

    private fun triggerPipeline(
        startType: StartType,
        projectId: String,
        pipelineId: String,
        buildId: String,
        startParameters: MutableMap<String, String>,
        pipelineInfo: PipelineInfo? = null,
        pipelineResourceVersion: PipelineResourceVersion? = null,
        userId: String
    ) = when (startType) {
        StartType.WEB_HOOK -> {
            // webhook触发
            webhookBuildParameterService.getBuildParameters(buildId = buildId)?.forEach { param ->
                startParameters[param.key] = param.value.toString()
            }
            // webhook触发
            BuildId(
                webhookTriggerPipelineBuild(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    parameters = startParameters,
                    checkPermission = false,
                    startType = startType,
                    pipelineInfo = pipelineInfo,
                    pipelineResource = pipelineResourceVersion
                )!!
            )
        }
        else -> {
            buildManualStartup(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS,
                values = startParameters,
                startType = startType
            )
        }
    }

    fun buildRestart(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): String {
        // 校验用户是否有执行流水线权限
        val permission = AuthPermission.EXECUTE
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(
                    userId,
                    projectId,
                    permission.getI18n(I18nUtil.getLanguage(userId)),
                    pipelineId
                )
            )
        )
        // 校验是否pipeline跟buildId匹配, 防止误传参数
        val buildInfo = checkPipelineInfo(projectId, pipelineId, buildId)
        // 防止接口有并发问题
        val redisLock = PipelineRefreshBuildLock(redisOperation, buildId)
        try {
            if (redisLock.tryLock()) {
                // 同一个buildId只能有一个在refresh的请求
                if (pipelineRedisService.getBuildRestartValue(buildId) != null) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.BAD_REQUEST.statusCode,
                        errorCode = ProcessMessageCode.ERROR_RESTART_EXSIT,
                        params = arrayOf(buildId)
                    )
                }

                // 目标构建已经结束,直接按原有启动参数新发起一次构建,此次构建会遵循流水线配置的串行阈值
                if (buildInfo.status.isFinish()) {
                    return buildRestartPipeline(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildInfo = buildInfo
                    )
                }

                // 锁定当前构建refresh操作
                pipelineRedisService.setBuildRestartValue(buildId)
                // 取消当次构建
                pipelineRuntimeService.cancelBuild(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    userId = userId,
                    executeCount = buildInfo.executeCount ?: 1,
                    buildStatus = BuildStatus.CANCELED
                )
                return buildRestartPipeline(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildInfo = buildInfo
                )
            }
        } finally {
            redisLock.unlock()
        }
        throw ErrorCodeException(
            statusCode = Response.Status.BAD_REQUEST.statusCode,
            errorCode = ProcessMessageCode.ERROR_RESTART_EXSIT,
            params = arrayOf(buildId)
        )
    }

    fun getBuildParamFormProp(
        projectId: String,
        pipelineId: String,
        includeConst: Boolean?,
        includeNotRequired: Boolean?,
        userId: String,
        version: Int?
    ): List<PipelineBuildParamFormProp> {
        val (pipeline, resource, debug) = pipelineRepositoryService.getBuildTriggerInfo(
            projectId, pipelineId, version
        )
        val model = resource.model
        val triggerContainer = model.getTriggerContainer()
        val properties = getBuildManualParams(
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            debug = false,
            checkPermission = true,
            triggerParams = triggerContainer.params
        )
        val parameter = ArrayList<PipelineBuildParamFormProp>()
        val prop = properties.filter {
            val const = if (includeConst == false) {
                it.constant != true
            } else {
                true
            }
            val required = if (includeNotRequired == false) {
                it.required
            } else {
                true
            }
            const && required
        }
        for (item in prop) {
            if (item.type == BuildFormPropertyType.MULTIPLE || item.type == BuildFormPropertyType.ENUM) {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                val defaultValue = item.defaultValue.toString()
                for (option in item.options!!) {
                    valueList.add(
                        StartUpInfo(
                            option.key,
                            option.value
                        )
                    )
                }
                val info = PipelineBuildParamFormProp(
                    key = item.id,
                    keyDisable = true,
                    keyType = "input",
                    keyListType = "",
                    keyUrl = "",
                    keyUrlQuery = ArrayList(),
                    keyList = keyList,
                    keyMultiple = false,
                    value = if (item.type == BuildFormPropertyType.MULTIPLE) {
                        if (defaultValue.isBlank()) {
                            ArrayList()
                        } else {
                            defaultValue.split(",")
                        }
                    } else {
                        defaultValue
                    },
                    valueDisable = false,
                    valueType = "select",
                    valueListType = "list",
                    valueUrl = "",
                    valueUrlQuery = ArrayList(),
                    valueList = valueList,
                    valueMultiple = item.type == BuildFormPropertyType.MULTIPLE,
                    type = item.type.value
                )
                parameter.add(info)
            } else {
                val keyList = ArrayList<StartUpInfo>()
                val valueList = ArrayList<StartUpInfo>()
                val info = PipelineBuildParamFormProp(
                    key = item.id,
                    keyDisable = true,
                    keyType = "input",
                    keyListType = "",
                    keyUrl = "",
                    keyUrlQuery = ArrayList(),
                    keyList = keyList,
                    keyMultiple = false,
                    value = item.defaultValue,
                    valueDisable = false,
                    valueType = "input",
                    valueListType = "",
                    valueUrl = "",
                    valueUrlQuery = ArrayList(),
                    valueList = valueList,
                    valueMultiple = false,
                    type = item.type.value
                )
                parameter.add(info)
            }
        }
        return parameter
    }

    private fun getBuildManualParams(
        projectId: String,
        pipelineId: String,
        userId: String?,
        debug: Boolean,
        checkPermission: Boolean,
        triggerParams: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        if (checkPermission) { // 不用校验查看权限，只校验执行权限
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId!!,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(
                        userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId
                    )
                )
            )
        }
        // #2902 默认增加构建信息
        val params = mutableListOf<BuildFormProperty>()
        if (!debug) params.add(
            BuildFormProperty(
                id = PIPELINE_BUILD_MSG,
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = "",
                value = "",
                options = null,
                desc = I18nUtil.getCodeLanMessage(
                    messageCode = ProcessMessageCode.BUILD_MSG_DESC,
                    language = I18nUtil.getLanguage(userId)
                ),
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null,
                label = I18nUtil.getCodeLanMessage(messageCode = ProcessMessageCode.BUILD_MSG_LABEL),
                placeholder = I18nUtil.getCodeLanMessage(messageCode = ProcessMessageCode.BUILD_MSG_MANUAL),
                propertyType = BuildPropertyType.BUILD.name
            )
        )
        params.addAll(
            filterParams(
                userId = if (checkPermission) userId else null,
                projectId = projectId,
                pipelineId = pipelineId,
                params = triggerParams
            )
        )

        BuildPropertyCompatibilityTools.fix(params)
        return params
    }

    private fun buildRestartPipeline(
        projectId: String,
        pipelineId: String,
        buildInfo: BuildInfo
    ): String {
        // 按原有的启动参数组装启动参数
        val startParameters = mutableMapOf<String, String>()
        buildInfo.buildParameters?.map {
            startParameters.put(it.key, it.value.toString())
        }
        val startType = StartType.toStartType(buildInfo.trigger)
        // 发起新构建
        return if (startType == StartType.WEB_HOOK) {
            webhookBuildParameterService.getBuildParameters(buildId = buildInfo.buildId)?.forEach { param ->
                startParameters[param.key] = param.value.toString()
            }
            webhookTriggerPipelineBuild(
                userId = buildInfo.startUser,
                projectId = projectId,
                pipelineId = pipelineId,
                parameters = startParameters,
                checkPermission = false,
                startType = startType
            )!!
        } else {
            // 发起新构建
            buildManualStartup(
                userId = buildInfo.startUser,
                startType = StartType.MANUAL,
                projectId = projectId,
                pipelineId = pipelineId,
                values = startParameters,
                channelCode = ChannelCode.BS
            ).id
        }
    }

    private fun checkPipelineInfo(projectId: String, pipelineId: String, buildId: String): BuildInfo {
        return pipelineRuntimeService.getBuildInfo(projectId, pipelineId, buildId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
    }

    private fun checkManualReviewParamOut(
        type: ManualReviewParamType,
        originParam: ManualReviewParam,
        param: String
    ) {
        when (type) {
            ManualReviewParamType.MULTIPLE -> {
                if (!originParam.options!!.map { it.key }.toList().containsAll(param.split(","))) {
                    throw ParamBlankException("param: ${originParam.key} value not in multipleParams")
                }
            }

            ManualReviewParamType.ENUM -> {
                if (!originParam.options!!.map { it.key }.toList().contains(param)) {
                    throw ParamBlankException("param: ${originParam.key} value not in enumParams")
                }
            }

            ManualReviewParamType.BOOLEAN -> {
                originParam.value = param.toBoolean()
            }

            ManualReviewParamType.CHECKBOX -> {
                val value = param.toBoolean()
                if (originParam.required && !value) {
                    throw ParamBlankException("param: ${originParam.key} value must be true")
                }
                originParam.value = value
            }

            else -> {
                originParam.value = param
            }
        }
    }

    private fun checkManualReviewParam(params: MutableList<ManualReviewParam>) {
        params.forEach { item ->
            val value = item.value.toString()
            if (item.required && value.isBlank()) {
                throw ParamBlankException("requiredParam: ${item.key}  is Null")
            }
            if (value.isBlank()) {
                return@forEach
            }
            checkManualReviewParamOut(item.valueType, item, value)
        }
    }
}
