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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.SimpleResult
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.BuildPropertyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.StageReviewRequest
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.utils.BuildStatusSwitcher
import com.tencent.devops.common.redis.RedisOperation
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
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.pojo.pipeline.ModelRecord
import com.tencent.devops.process.pojo.pipeline.PipelineLatestBuild
import com.tencent.devops.process.service.BuildVariableService
import com.tencent.devops.process.service.ParamFacadeService
import com.tencent.devops.process.service.PipelineTaskPauseService
import com.tencent.devops.process.service.pipeline.PipelineBuildService
import com.tencent.devops.process.util.TaskUtils
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_ALL_FAILED_CONTAINER
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_SKIP_FAILED_TASK
import com.tencent.devops.process.utils.PIPELINE_START_TASK_ID
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

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
    private val pipelineTaskPauseService: PipelineTaskPauseService,
    private val containerBuildRecordService: ContainerBuildRecordService,
    private val jmxApi: ProcessJmxApi,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineBuildQualityService: PipelineBuildQualityService,
    private val paramFacadeService: ParamFacadeService,
    private val buildLogPrinter: BuildLogPrinter,
    private val buildParamCompatibilityTransformer: BuildParametersCompatibilityTransformer,
    private val pipelineRedisService: PipelineRedisService,
    private val pipelineRetryFacadeService: PipelineRetryFacadeService,
    private val webhookBuildParameterService: WebhookBuildParameterService
) {

    @Value("\${pipeline.build.cancel.intervalLimitTime:60}")
    private var cancelIntervalLimitTime: Int = 60 // 取消间隔时间为60秒

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildFacadeService::class.java)
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
        checkPermission: Boolean = true
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

        pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )

        val model = getModel(projectId, pipelineId)

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        var canManualStartup = false
        var canElementSkip = false
        var useLatestParameters = false
        run lit@{
            triggerContainer.elements.forEach {
                if (it is ManualTriggerElement && it.isElementEnable()) {
                    canManualStartup = true
                    canElementSkip = it.canElementSkip ?: false
                    useLatestParameters = it.useLatestParameters ?: false
                    return@lit
                }
            }
        }

        // 当使用最近一次参数进行构建的时候，获取并替换container.params中的defaultValue值
        if (useLatestParameters) {
            // 获取最后一次的构建id
            val lastTimeBuildInfo = pipelineRuntimeService.getLastTimeBuild(projectId, pipelineId)
            if (lastTimeBuildInfo?.buildParameters?.isNotEmpty() == true) {
                val latestParamsMap = lastTimeBuildInfo.buildParameters!!.associate { it.key to it.value }
                triggerContainer.params.forEach { param ->
                    val realValue = latestParamsMap[param.id]
                    if (realValue != null) {
                        // 有上一次的构建参数的时候才设置成默认值，否者依然使用默认值。
                        // 当值是boolean类型的时候，需要转为boolean类型
                        if (param.defaultValue is Boolean) {
                            param.defaultValue = realValue.toString().toBoolean()
                        } else {
                            param.defaultValue = realValue
                        }
                    }
                }
            }
        }

        // #2902 默认增加构建信息
        val params = mutableListOf(
            BuildFormProperty(
                id = PIPELINE_BUILD_MSG,
                required = true,
                type = BuildFormPropertyType.STRING,
                defaultValue = "",
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
                params = triggerContainer.params
            )
        )

        BuildPropertyCompatibilityTools.fix(params)

        val currentBuildNo = triggerContainer.buildNo
        if (currentBuildNo != null) {
            currentBuildNo.buildNo = pipelineRepositoryService.getBuildNo(
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: currentBuildNo.buildNo
        }

        return BuildManualStartupInfo(
            canManualStartup = canManualStartup,
            canElementSkip = canElementSkip,
            properties = params,
            buildNo = currentBuildNo
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
        buildId: String
    ): List<BuildParameters> {

        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.VIEW,
            message = MessageUtil.getMessageByLocale(
                ERROR_USER_NO_PERMISSION_GET_PIPELINE_INFO,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, pipelineId, "")
            )
        )
        return pipelineRuntimeService.getBuildParametersFromStartup(projectId, buildId)
    }

    fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String, // 要重试的构建ID
        taskId: String? = null, // 要重试或跳过的插件ID，或者StageId
        failedContainer: Boolean? = false, // 仅重试所有失败Job
        skipFailedTask: Boolean? = false, // 跳过失败插件，为true时需要传taskId值（值为stageId则表示跳过Stage下所有失败插件）
        isMobile: Boolean = false,
        channelCode: ChannelCode? = ChannelCode.BS,
        checkPermission: Boolean? = true,
        checkManualStartup: Boolean? = false
    ): BuildId {
        if (checkPermission!!) {
            val permission = AuthPermission.EXECUTE
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission,
                message = MessageUtil.getMessageByLocale(
                    CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                    I18nUtil.getLanguage(userId),
                    arrayOf(userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId)
                )
            )
        }

        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        try {

            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
                ?: throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )

            if (buildInfo.pipelineId != pipelineId) {
                throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT)
            }

            // 运行中的task重试走全新的处理逻辑
            if (!buildInfo.isFinish()) {
                if (pipelineRetryFacadeService.runningBuildTaskRetry(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = buildId,
                        executeCount = buildInfo.executeCount ?: 1,
                        resourceVersion = buildInfo.version,
                        taskId = taskId,
                        skipFailedTask = skipFailedTask
                    )
                ) {
                    return BuildId(buildId, buildInfo.executeCount ?: 1, projectId, pipelineId)
                }

                // 对不合法的重试进行拦截，防止重复提交
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_DUPLICATE_BUILD_RETRY_ACT
                )
            }

            val readyToBuildPipelineInfo =
                pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
                    ?: throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                        params = arrayOf(buildId)
                    )

            if (!readyToBuildPipelineInfo.canManualStartup && checkManualStartup == false) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.DENY_START_BY_MANUAL
                )
            }

            val model = buildDetailService.getBuildModel(projectId, buildId) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
            )

            val paramMap = HashMap<String, BuildParameters>(100, 1F)
            val webHookStartParam = HashMap<String, BuildParameters>(100, 1F)
            // #2821 构建重试均要传递触发插件ID，否则当有多个事件触发插件时，rebuild后触发器的标记不对
            buildVariableService.getVariable(
                projectId, pipelineId, buildId, PIPELINE_START_TASK_ID
            )?.let { startTaskId ->
                paramMap[PIPELINE_START_TASK_ID] = BuildParameters(PIPELINE_START_TASK_ID, startTaskId)
            }

            val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)
            buildInfo.buildParameters?.forEach { param -> webHookStartParam[param.key] = param }
            webhookBuildParameterService.getBuildParameters(buildId)?.forEach { param ->
                webHookStartParam[param.key] = param
            }
            val startType = StartType.toStartType(buildInfo.trigger)
            if (!taskId.isNullOrBlank()) {
                buildInfo.buildParameters?.associateBy { it.key }?.get(PIPELINE_RETRY_COUNT)?.let { param ->
                    paramMap[param.key] = param
                }
                // stage/job/task级重试
                run {
                    model.stages.forEach { s ->
                        // stage 级重试
                        if (s.id == taskId) {
                            val stage = pipelineStageService.getStage(projectId, buildId, stageId = s.id) ?: run {
                                throw ErrorCodeException(
                                    errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
                                )
                            }
                            // 只有失败或取消情况下提供重试得可能
                            if (!stage.status.isFailure() && !stage.status.isCancel()) throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_RETRY_STAGE_NOT_FAILED
                            )
                            paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
                                key = PIPELINE_RETRY_START_TASK_ID, value = s.id!!
                            )
                            paramMap[PIPELINE_RETRY_ALL_FAILED_CONTAINER] = BuildParameters(
                                key = PIPELINE_RETRY_ALL_FAILED_CONTAINER,
                                value = failedContainer ?: false,
                                valueType = BuildFormPropertyType.TEMPORARY
                            )
                            paramMap[PIPELINE_SKIP_FAILED_TASK] = BuildParameters(
                                key = PIPELINE_SKIP_FAILED_TASK,
                                value = skipFailedTask ?: false,
                                valueType = BuildFormPropertyType.TEMPORARY
                            )
                            return@run
                        }
                        s.containers.forEach { c ->
                            val pos = if (c.id == taskId) 0 else -1 // 容器job级别的重试，则找job的第一个原子
                            c.elements.forEachIndexed { index, element ->
                                if (index == pos) {
                                    pipelineContainerService.getContainer(projectId, buildId, s.id, c.id!!) ?: run {
                                        throw ErrorCodeException(
                                            errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
                                        )
                                    }
                                    paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
                                        key = PIPELINE_RETRY_START_TASK_ID, value = element.id!!
                                    )

                                    return@run
                                }
                                if (element.id == taskId) {
                                    pipelineTaskService.getByTaskId(null, projectId, buildId, taskId)
                                        ?: run {
                                            throw ErrorCodeException(
                                                errorCode = ProcessMessageCode.ERROR_BUILD_EXPIRED_CANT_RETRY
                                            )
                                        }
                                    paramMap[PIPELINE_RETRY_START_TASK_ID] = BuildParameters(
                                        key = PIPELINE_RETRY_START_TASK_ID, value = element.id!!
                                    )
                                    paramMap[PIPELINE_SKIP_FAILED_TASK] = BuildParameters(
                                        key = PIPELINE_SKIP_FAILED_TASK,
                                        value = skipFailedTask ?: false,
                                        valueType = BuildFormPropertyType.TEMPORARY
                                    )
                                    return@run
                                }
                            }
                        }
                    }
                }
            } else {
                // 完整构建重试，去掉启动参数中的重试插件ID保证不冲突，同时保留重试次数，并清理VAR表内容
                try {
                    paramMap.putAll(webHookStartParam)
                    if (setting?.cleanVariablesWhenRetry == true) {
                        buildVariableService.deleteBuildVars(projectId, pipelineId, buildId)
                    }
                } catch (ignored: Exception) {
                    logger.warn("ENGINE|$buildId|Fail to get the startup param: $ignored")
                }
            }

            // 重置因暂停而变化的element(需同时支持流水线重试和stage重试, task重试), model不在这保存，在startBuild中保存
            pipelineTaskPauseService.resetElementWhenPauseRetry(projectId, buildId, model)

            // rebuild重试计数
            val retryCount = (paramMap[PIPELINE_RETRY_COUNT]?.value?.toString()?.toInt() ?: 0) + 1

            logger.info(
                "ENGINE|$buildId|RETRY_PIPELINE_ORIGIN|taskId=$taskId|$pipelineId|" +
                    "retryCount=$retryCount|fc=$failedContainer|skip=$skipFailedTask"
            )

            paramMap[PIPELINE_RETRY_COUNT] = BuildParameters(PIPELINE_RETRY_COUNT, retryCount)
            paramMap[PIPELINE_RETRY_BUILD_ID] = BuildParameters(PIPELINE_RETRY_BUILD_ID, buildId, readOnly = true)

            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = readyToBuildPipelineInfo,
                startType = startType,
                pipelineParamMap = paramMap,
                channelCode = channelCode ?: ChannelCode.BS,
                isMobile = isMobile,
                model = model,
                signPipelineVersion = buildInfo.version,
                frequencyLimit = true,
                handlePostFlag = false,
                webHookStartParam = webHookStartParam
            )
        } finally {
            redisLock.unlock()
        }
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
        triggerReviewers: List<String>? = null
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

        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )

        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId, pipelineId)

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer

            if (startType == StartType.MANUAL) {
                if (!readyToBuildPipelineInfo.canManualStartup) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.DENY_START_BY_MANUAL
                    )
                }
            } else if (startType == StartType.REMOTE) {
                var canRemoteStartup = false
                run lit@{
                    triggerContainer.elements.forEach {
                        if (it is RemoteTriggerElement && it.isElementEnable()) {
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
                pipelineRuntimeService.updateBuildNo(projectId, pipelineId, buildNo)
                logger.info("[$pipelineId] buildNo was changed to [$buildNo]")
            }

            val paramMap = buildParamCompatibilityTransformer.parseTriggerParam(triggerContainer.params, values)

            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = readyToBuildPipelineInfo,
                startType = startType,
                pipelineParamMap = paramMap,
                channelCode = channelCode,
                isMobile = isMobile,
                model = model,
                frequencyLimit = frequencyLimit,
                buildNo = buildNo,
                startValues = values,
                triggerReviewers = triggerReviewers
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
        val pipeline = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId = pipelineId) ?: return null
        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId = projectId, pipelineId = pipelineId, version = pipeline.version)

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer

            val paramPamp = buildParamCompatibilityTransformer.parseTriggerParam(triggerContainer.params, parameters)

            return pipelineBuildService.startPipeline(
                userId = userId,
                pipeline = pipeline,
                startType = StartType.TIME_TRIGGER,
                pipelineParamMap = paramPamp,
                channelCode = pipeline.channelCode,
                isMobile = false,
                model = model,
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
        triggerReviewers: List<String>? = null
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
        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return null
        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId, pipelineId, readyToBuildPipelineInfo.version)

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer

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
                model = model,
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
        checkPermission: Boolean = true
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
            channelCode = channelCode
        )
    }

    fun buildManualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        params: ReviewParam,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
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

        model.stages.forEachIndexed { index, s ->
            if (index == 0) {
                return@forEachIndexed
            }
            s.containers.forEach { cc ->
                cc.elements.forEach { el ->
                    if (el is ManualReviewUserTaskElement && el.id == elementId) {
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
                                ManualReviewParamType.BOOLEAN -> {
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
        logger.info("[$buildId]|buildManualReview|taskId=$elementId|userId=$userId|params=$params")

        pipelineRuntimeService.manualDealReview(
            taskId = elementId,
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
        val executeCount = buildInfo.executeCount ?: 1
        if (approve) {
            pipelineRuntimeService.approveTriggerReview(userId = userId, buildInfo = buildInfo)
        } else {
            pipelineRuntimeService.disapproveTriggerReview(
                userId = userId, buildId = buildId, pipelineId = pipelineId,
                projectId = projectId, executeCount = executeCount
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
                    maxConRunningQueueSize = setting.maxConRunningQueueSize
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
                    reviewRequest = reviewRequest
                )
            } else {
                pipelineStageService.stageManualStart(
                    userId = userId,
                    buildStage = buildStage,
                    reviewRequest = reviewRequest
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
                                ManualReviewParamType.BOOLEAN -> {
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

    fun getBuildDetailByBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: Int,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
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
        val buildId = pipelineRuntimeService.getBuildIdByBuildNum(projectId, pipelineId, buildNo)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf("buildNo=$buildNo")
            )
        return getBuildDetail(
            projectId = projectId, pipelineId = pipelineId, buildId = buildId, channelCode = channelCode
        )
    }

    fun getBuildRecordByBuildNum(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNum: Int,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
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
        val buildId = pipelineRuntimeService.getBuildIdByBuildNum(projectId, pipelineId, buildNum)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                params = arrayOf("buildNum=$buildNum")
            )
        return getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = null,
            channelCode = channelCode
        )
    }

    fun getBuildRecord(
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        channelCode: ChannelCode
    ): ModelRecord {
        val buildInfo = pipelineRuntimeService.getBuildInfo(
            projectId = projectId,
            buildId = buildId
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
        if (projectId != buildInfo.projectId || pipelineId != buildInfo.pipelineId) {
            throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID,
                params = arrayOf(buildId)
            )
        }
        return buildRecordService.getBuildRecord(
            buildInfo = buildInfo,
            executeCount = executeCount
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
            params = arrayOf(buildId)
        )
    }

    fun getBuildRecord(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        executeCount: Int?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): ModelRecord {

        if (checkPermission) {
            pipelinePermissionService.validPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.VIEW,
                message = null
            )
        }

        return getBuildRecord(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            executeCount = executeCount,
            channelCode = channelCode
        )
    }

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
        pipelineRuntimeService.getPipelineBuildHistoryCount(
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
        checkPermission: Boolean
    ): List<String> {
        return pipelineRuntimeService.getBuilds(
            projectId, pipelineId, buildStatus
        )
    }

    fun getHistoryBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true,
        updateTimeDesc: Boolean? = null
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

            val newTotalCount = pipelineRuntimeService.getPipelineBuildHistoryCount(projectId, pipelineId)
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
        updateTimeDesc: Boolean? = null
    ): BuildHistoryPage<BuildHistory> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 50
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 1000

        val channelCode = if (projectId.startsWith("git_")) ChannelCode.GIT else ChannelCode.BS

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
                startUser = startUser
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
                updateTimeDesc = updateTimeDesc
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

    fun getHistoryConditionRepo(userId: String, projectId: String, pipelineId: String): List<String> {
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
        return pipelineRuntimeService.getHistoryConditionRepo(projectId, pipelineId)
    }

    fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?
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
        return pipelineRuntimeService.getHistoryConditionBranch(projectId, pipelineId, alias)
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
            pipelineVersion = build.version
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
        val buildHistory = pipelineRuntimeService.getBuildHistoryByBuildNum(
            projectId = projectId,
            pipelineId = pipelineId,
            buildNum = buildNum,
            statusSet = statusSet
        )
        logger.info("[$pipelineId]|buildHistory=$buildHistory")
        return buildHistory
    }

    fun getLatestSuccessBuild(
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode
    ): BuildHistory? {
        val buildHistory = pipelineRuntimeService.getBuildHistoryByBuildNum(
            projectId = projectId,
            pipelineId = pipelineId,
            buildNum = -1,
            statusSet = setOf(BuildStatus.SUCCEED)
        )
        logger.info("[$pipelineId]|buildHistory=$buildHistory")
        return buildHistory
    }

    fun getModel(projectId: String, pipelineId: String, version: Int? = null) =
        pipelineRepositoryService.getModel(projectId, pipelineId, version) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

    private fun buildManualShutdown(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        channelCode: ChannelCode
    ) {

        val redisLock = BuildIdLock(redisOperation = redisOperation, buildId = buildId)
        try {
            redisLock.lock()

            val modelDetail = buildDetailService.get(projectId, buildId) ?: return
            val alreadyCancelUser = modelDetail.cancelUserId

            if (BuildStatus.parse(modelDetail.status).isFinish()) {
                logger.warn("The build $buildId of project $projectId already finished ")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.PIPELINE_BUILD_HAS_ENDED_CANNOT_BE_CANCELED
                )
            }

            if (modelDetail.pipelineId != pipelineId) {
                logger.warn("shutdown error: input|$pipelineId| buildId-pipeline| ${modelDetail.pipelineId}| $buildId")
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPLEINE_INPUT
                )
            }
            // 兼容post任务的场景，处于”运行中“的构建可以支持多次取消操作(第二次取消直接强制终止流水线构建)
            val cancelActionTime = redisOperation.get(BuildUtils.getCancelActionBuildKey(buildId))?.toLong() ?: 0
            val intervalTime = System.currentTimeMillis() - cancelActionTime
            var terminateFlag = false // 是否强制终止
            if (intervalTime <= cancelIntervalLimitTime * 1000) {
                logger.warn("The build $buildId of project $projectId already cancel by user $alreadyCancelUser")
                val timeTip = cancelIntervalLimitTime - intervalTime / 1000
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.CANCEL_BUILD_BY_OTHER_USER,
                    params = arrayOf(userId, timeTip.toString())
                )
            } else if (cancelActionTime > 0) {
                terminateFlag = true
            }

            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)

            if (pipelineInfo == null) {
                logger.warn("The pipeline($pipelineId) of project($projectId) is not exist")
                return
            }
            if (pipelineInfo.channelCode != channelCode) {
                return
            }

            val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            if (buildInfo == null) {
                logger.warn("The build($buildId) of pipeline($pipelineId) is not exist")
                throw ErrorCodeException(
                    statusCode = Response.Status.NOT_FOUND.statusCode,
                    errorCode = ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID,
                    params = arrayOf(buildId)
                )
            }

            val tasks = pipelineTaskService.getRunningTask(projectId, buildId)

            tasks.forEach { task ->
                val taskId = task["taskId"]?.toString() ?: ""
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
                    jobId = containerId,
                    executeCount = executeCount
                )
            }

            if (tasks.isEmpty()) {
                val jobId = "0"
                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = "Cancelled by $userId",
                    tag = VMUtils.genStartVMTaskId(jobId),
                    jobId = jobId,
                    executeCount = 1
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
                    terminateFlag = terminateFlag
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

    fun workerBuildFinish(
        projectCode: String,
        pipelineId: String, /* pipelineId在agent请求的数据有值前不可用 */
        buildId: String,
        vmSeqId: String,
        nodeHashId: String?,
        simpleResult: SimpleResult
    ) {
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
                        jobId = startUpVMTask.containerHashId,
                        executeCount = startUpVMTask.executeCount ?: 1
                    )
                }
                return
            }
        } else {
            msg = "$msg| ${I18nUtil.getCodeLanMessage(ProcessMessageCode.BUILD_WORKER_DEAD_ERROR)}"
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
            return
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
                        actionType = ActionType.TERMINATE,
                        reason = msg,
                        errorCode = simpleResult.error?.errorCode ?: 0,
                        errorTypeName = realErrorType?.getI18n(I18nUtil.getDefaultLocaleLanguage())
                    )
                )
            }
        }
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
