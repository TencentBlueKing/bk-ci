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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.ManualReviewAction
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.pojo.element.agent.ManualReviewUserTaskElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.RemoteTriggerElement
import com.tencent.devops.common.pipeline.utils.SkipElementUtils
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_DUPLICATE_BUILD_RETRY_ACT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_BUILD_EXISTS_BY_ID
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NO_PIPELINE_EXISTS_BY_ID
import com.tencent.devops.process.engine.interceptor.InterceptData
import com.tencent.devops.process.engine.interceptor.PipelineInterceptorChain
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.jmx.api.ProcessJmxApi
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildHistoryVariables
import com.tencent.devops.process.pojo.BuildHistoryWithPipelineVersion
import com.tencent.devops.process.pojo.BuildManualStartupInfo
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.process.service.BuildStartupParamService
import com.tencent.devops.process.service.ParamService
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_RETRY_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_RETRY_COUNT
import com.tencent.devops.process.utils.PIPELINE_RETRY_START_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriBuilder

/**
 *
 * @version 1.0
 */
@Service
class PipelineBuildService(
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val redisOperation: RedisOperation,
    private val buildDetailService: PipelineBuildDetailService,
    private val jmxApi: ProcessJmxApi,
    private val pipelinePermissionService: PipelinePermissionService,
    private val buildStartupParamService: BuildStartupParamService,
    private val paramService: ParamService,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildService::class.java)
        private val NO_LIMIT_CHANNEL = listOf(ChannelCode.CODECC)
    }

    private fun checkPermission(userId: String, projectId: String, pipelineId: String, message: String) =
        checkPermission(userId, projectId, pipelineId, BkAuthPermission.EXECUTE, message)

    private fun checkPermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: BkAuthPermission,
        message: String
    ) {
        if (!pipelinePermissionService.checkPipelinePermission(userId, projectId, pipelineId, permission)) {
            throw PermissionForbiddenException(message)
        }
    }

    private fun filterParams(
        userId: String?,
        projectId: String,
        params: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        return paramService.filterParams(userId, projectId, params)
    }

    private fun hasDownloadPermission(userId: String, projectId: String, pipelineId: String): Boolean {
        return pipelinePermissionService.checkPipelinePermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.EXECUTE
        )
    }

    fun buildManualStartupInfo(
        userId: String?,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): BuildManualStartupInfo {

        if (checkPermission) { // 不用校验查看权限，只校验执行权限
            checkPermission(userId!!, projectId, pipelineId, "用户（$userId) 无权限启动流水线($pipelineId)")
        }

        pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw NotFoundException("流水线不存在")

        val model = getModel(projectId, pipelineId)

        val container = model.stages[0].containers[0] as TriggerContainer

        var canManualStartup = false
        var canElementSkip = false
        var useLatestParameters = false
        run lit@{
            container.elements.forEach {
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
            val lastTimeBuildInfo = pipelineRuntimeService.getLastTimeBuild(pipelineId)
            if (lastTimeBuildInfo != null) {
                val latestParamsStr = buildStartupParamService.getParam(lastTimeBuildInfo.buildId)
                // 为空的时候不处理
                if (latestParamsStr != null) {
                    val latestParams =
                        JsonUtil.to(latestParamsStr, object : TypeReference<MutableMap<String, Any>>() {})
                    container.params.forEach { param ->
                        val realValue = latestParams[param.id]
                        if (realValue != null) {
                            // 有上一次的构建参数的时候才设置成默认值，否者依然使用默认值。
                            // 当值是boolean类型的时候，需要转为boolean类型
                            if (param.defaultValue is Boolean) {
                                param.defaultValue = realValue.toString().toBoolean()
                            } else {
                                param.defaultValue = realValue
                            }
                        }
//                        latestParams.forEach { latestParam ->
//                            if (param.id == latestParam.key) {
//                                // 有上一次的构建参数的时候才设置成默认值，否者依然使用默认值。
//                                // 当值是boolean类型的时候，需要转为boolean类型
//                                var realValue = latestParam.value
//                                if (param.defaultValue is Boolean) {
//                                    realValue = when (realValue) {
//                                        "false" -> false
//                                        "true" -> true
//                                        else -> null
//                                    }
//                                }
//                                param.defaultValue = realValue ?: param.defaultValue
//                            }
//                        }
                    }
                }
            }
        }

        val params = filterParams(if (checkPermission && userId != null) userId else null, projectId, container.params)

        return BuildManualStartupInfo(canManualStartup, canElementSkip, params)
    }

    fun getBuildParameters(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String
    ): List<BuildParameters> {

        checkPermission(userId, projectId, pipelineId, BkAuthPermission.VIEW, "用户（$userId) 无权限获取流水线($pipelineId)信息")

        return try {
            val startupParam = buildStartupParamService.getParam(buildId)
            if (startupParam == null || startupParam.isEmpty()) {
                emptyList()
            } else {
                try {
                    val map: Map<String, Any> = JsonUtil.toMap(startupParam)
                    map.map { transform ->
                        BuildParameters(transform.key, transform.value)
                    }.toList().filter { !it.key.startsWith(SkipElementUtils.prefix) }
                } catch (e: Exception) {
                    logger.warn("Fail to convert the parameters($startupParam) to map of build($buildId)", e)
                    throw e
                }
            }
        } catch (e: NotFoundException) {
            return emptyList()
        }
    }

    fun retry(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        taskId: String? = null,
        isMobile: Boolean = false
    ): String {

        checkPermission(userId, projectId, pipelineId, "用户（$userId) 无权限重启流水线($pipelineId)")

        val redisLock = RedisLock(redisOperation, "build:concurrency:$buildId", 30L)
        try {

            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
                ?: throw NotFoundException("构建不存在")

            if (!BuildStatus.isFailure(buildInfo.status)) {
                throw ErrorCodeException(ERROR_DUPLICATE_BUILD_RETRY_ACT.toString(), "重试已经启动，忽略重复的请求")
            }

            val model = getModel(projectId, pipelineId, buildInfo.version)

            val container = model.stages[0].containers[0] as TriggerContainer

            var canManualStartup = false
            run lit@{
                container.elements.forEach {
                    if (it is ManualTriggerElement && it.isElementEnable()) {
                        canManualStartup = true
                        return@lit
                    }
                }
            }
            if (!canManualStartup) {
                throw OperationException("该流水线不能手动启动")
            }
            val params = mutableMapOf<String, Any>()
            if (!taskId.isNullOrBlank()) {
                // job/task级重试，获取buildVariable构建参数，恢复环境变量
                params.putAll(pipelineRuntimeService.getAllVariable(buildId))
                // job/task级重试
                run {
                    model.stages.forEach { s ->
                        s.containers.forEach { c ->
                            val pos = if (c.id == taskId) 0 else -1 // 容器job级别的重试，则找job的第一个原子
                            c.elements.forEachIndexed { index, element ->
                                if (index == pos) {
                                    params[PIPELINE_RETRY_START_TASK_ID] = element.id!!
                                    return@run
                                }
                                if (element.id == taskId) {
                                    params[PIPELINE_RETRY_START_TASK_ID] = taskId!!
                                    return@run
                                }
                            }
                        }
                    }
                }

                params[PIPELINE_RETRY_COUNT] = if (params[PIPELINE_RETRY_COUNT] != null) {
                    params[PIPELINE_RETRY_COUNT].toString().toInt() + 1
                } else {
                    1
                }
            } else {
                // 完整构建重试
                try {
                    val startupParam = buildStartupParamService.getParam(buildId)
                    if (startupParam != null && startupParam.isNotEmpty()) {
                        params.putAll(JsonUtil.toMap(startupParam))
                    }
                } catch (e: Exception) {
                    logger.warn("Fail to get the startup param for the build($buildId)", e)
                }
                // 假如之前构建有原子级重试，则清除掉。因为整个流水线重试的是一个新的构建了(buildId)。
                params.remove(PIPELINE_RETRY_COUNT)
            }

            params[PIPELINE_START_USER_ID] = userId
            params[PIPELINE_START_TYPE] = StartType.MANUAL.name
            params[PIPELINE_RETRY_BUILD_ID] = buildId

            val readyToBuildPipelineInfo =
                pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, ChannelCode.BS)
                    ?: throw NotFoundException("流水线数据异常，请刷新页面后重试")

            return startPipeline(
                userId,
                readyToBuildPipelineInfo,
                StartType.MANUAL,
                params,
                ChannelCode.BS,
                isMobile,
                model,
                buildInfo.version
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
        startByMessage: String? = null
    ): String {

        if (checkPermission) {
            checkPermission(userId, projectId, pipelineId, "用户（$userId) 无权限启动流水线($pipelineId)")
        }

        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw NotFoundException("流水线不存在")
        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId, pipelineId)

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer

            if (startType == StartType.MANUAL) {
                var canManualStartup = false
                run lit@{
                    triggerContainer.elements.forEach {
                        if (it is ManualTriggerElement && it.isElementEnable()) {
                            canManualStartup = true
                            return@lit
                        }
                    }
                }

                if (!canManualStartup) {
                    throw OperationException("该流水线不能手动启动")
                }
            }
            if (startType == StartType.REMOTE) {
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
                    throw OperationException("该流水线不能远程触发")
                }
            }

            val startParams = mutableMapOf<String, Any>()

            triggerContainer.params.forEach {
                val v = values[it.id]
                if (v == null) {
                    if (it.required) {
                        throw OperationException("参数(${it.id})是必填启动参数")
                    }
                    startParams[it.id] = it.defaultValue
                } else {
                    startParams[it.id] = v
                }
            }

            model.stages.forEachIndexed { index, stage ->
                if (index == 0) {
                    return@forEachIndexed
                }
                stage.containers.forEach { container ->
                    container.elements.forEach { e ->
                        values.forEach { value ->
                            val key = SkipElementUtils.getSkipElementVariableName(e.id)
                            if (value.key == key && value.value == "true") {
                                logger.info("${e.id} will be skipped.")
                                startParams[key] = "true"
                            }
                        }
                    }
                }
            }

            return startPipeline(userId, readyToBuildPipelineInfo, startType, startParams, channelCode, isMobile, model)
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to start pipeline($pipelineId)")
        }
    }

    fun subpipelineStartup(
        userId: String,
        startType: StartType,
        projectId: String,
        parentPipelineId: String,
        parentBuildId: String,
        parentTaskId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        parameters: Map<String, Any>,
        checkPermission: Boolean = true,
        isMobile: Boolean = false
    ): String {

        if (checkPermission) {
            checkPermission(userId, projectId, pipelineId, "用户（$userId) 无权限启动流水线($pipelineId)")
        }
        val readyToBuildPipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw NotFoundException("流水线不存在")
        val startEpoch = System.currentTimeMillis()
        try {

            val model = getModel(projectId, pipelineId, readyToBuildPipelineInfo.version)

            /**
             * 验证流水线参数构建启动参数
             */
            val triggerContainer = model.stages[0].containers[0] as TriggerContainer

            val startParams = mutableMapOf<String, Any>()
            startParams.putAll(parameters)

            triggerContainer.params.forEach {
                if (startParams.containsKey(it.id)) {
                    return@forEach
                }
                startParams[it.id] = it.defaultValue
            }
            startParams[PIPELINE_START_PIPELINE_USER_ID] = userId
            startParams[PIPELINE_START_PARENT_PIPELINE_ID] = parentPipelineId
            startParams[PIPELINE_START_PARENT_BUILD_ID] = parentBuildId
            startParams[PIPELINE_START_PARENT_BUILD_TASK_ID] = parentTaskId
            // 子流水线的调用不受频率限制
            val subBuildId = startPipeline(
                readyToBuildPipelineInfo.lastModifyUser, readyToBuildPipelineInfo,
                startType, startParams, channelCode, isMobile, model, null, false
            )
            // 更新父流水线关联子流水线构建id
            pipelineRuntimeService.updateTaskSubBuildId(parentBuildId, parentTaskId, subBuildId)
            return subBuildId
        } finally {
            logger.info("It take(${System.currentTimeMillis() - startEpoch})ms to start sub-pipeline($pipelineId)")
        }
    }

    /**
     * 定时触发
     */
    fun timerTriggerPipelineBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        parameters: Map<String, Any> = emptyMap(),
        checkPermission: Boolean = true
    ): String? {

        if (checkPermission) {
            checkPermission(userId, projectId, pipelineId, "用户（$userId) 无权限启动流水线($pipelineId)")
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

            val startParams = mutableMapOf<String, Any>()
            startParams.putAll(parameters)

            triggerContainer.params.forEach {
                if (startParams.containsKey(it.id)) {
                    return@forEach
                }
                startParams[it.id] = it.defaultValue
            }
            // 子流水线的调用不受频率限制
            return startPipeline(
                userId, readyToBuildPipelineInfo,
                StartType.TIME_TRIGGER, startParams, readyToBuildPipelineInfo.channelCode, false, model, null, false
            )
        } finally {
            logger.info("Timer| It take(${System.currentTimeMillis() - startEpoch})ms to start pipeline($pipelineId)")
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
            checkPermission(userId, projectId, pipelineId, "用户（$userId) 无权限停止流水线($pipelineId)")
        }

        buildManualShutdown(projectId, pipelineId, buildId, userId, channelCode)
    }

    fun buildManualReview(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        elementId: String,
        action: ManualReviewAction,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ) {

        pipelineRuntimeService.getBuildInfo(buildId)
            ?: throw NotFoundException("流水线构建不存在")

        val model = pipelineRepositoryService.getModel(pipelineId) ?: throw NotFoundException("流水线模型编排不存在")

        val runtimeVars = pipelineRuntimeService.getAllVariable(buildId)
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
                            reviewUser.addAll(EnvUtils.parseEnv(user, runtimeVars).split(","))
                        }
//                        elementName = el.name
                        if (!reviewUser.contains(userId)) {
                            logger.warn("User does not have the permission to review, userId:($userId)")
                            throw PermissionForbiddenException("用户（$userId) 无权限审核流水线($pipelineId)")
                        }
                    }
                }
            }
        }
        logger.info("[$buildId]|buildManualReview|taskId=$elementId|userId=$userId|action=$action")
        pipelineRuntimeService.manualDealBuildTask(buildId, elementId, userId, action)
        if (action == ManualReviewAction.ABORT) {
            buildDetailService.updateBuildCancelUser(buildId, userId)
        }
    }

    fun serviceShutdown(projectId: String, pipelineId: String, buildId: String, channelCode: ChannelCode) {
        val redisLock = RedisLock(redisOperation, "process.pipeline.build.shutdown.$buildId", 10)
        try {
            redisLock.lock()

            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)

            if (buildInfo == null) {
                logger.warn("[$buildId]|SERVICE_SHUTDOWN| not exist")
                return
            } else {
                if (buildInfo.parentBuildId != null && buildInfo.parentBuildId != buildId) {
                    if (StartType.PIPELINE.name == buildInfo.trigger) {
                        if (buildInfo.parentTaskId != null) {
                            val superPipeline = pipelineRuntimeService.getBuildInfo(buildInfo.parentBuildId!!)
                            if (superPipeline != null) {
                                logger.info("[$pipelineId]|SERVICE_SHUTDOWN|super_build=${superPipeline.buildId}|super_pipeline=${superPipeline.pipelineId}")
                                serviceShutdown(
                                    projectId,
                                    superPipeline.pipelineId,
                                    superPipeline.buildId,
                                    channelCode
                                )
                                return
                            }
                        }
                    }
                }
            }

            try {
                pipelineRuntimeService.cancelBuild(
                    projectId,
                    pipelineId,
                    buildId,
                    buildInfo.startUser,
                    BuildStatus.FAILED
                )
                buildDetailService.updateBuildCancelUser(buildId, buildInfo.startUser)
                logger.info("Cancel the pipeline($pipelineId) of instance($buildId) by the user(${buildInfo.startUser})")
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
            checkPermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = BkAuthPermission.VIEW,
                message = "用户（$userId) 无权限获取流水线($pipelineId)详情"
            )
        }

        return getBuildDetail(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            channelCode = channelCode,
            checkPermission = checkPermission
        )
    }

    fun getBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): ModelDetail {

        return buildDetailService.get(buildId) ?: throw NotFoundException("流水线编排不存在")
    }

    fun getBuildDetailByBuildNo(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildNo: Int,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): ModelDetail {
        checkPermission(userId, projectId, pipelineId, BkAuthPermission.VIEW, "用户（$userId) 无权限获取流水线($pipelineId)详情")
        val buildId = pipelineRuntimeService.getBuildIdbyBuildNo(projectId, pipelineId, buildNo)
            ?: throw NotFoundException("构建号($buildNo)不存在")
        return getBuildDetail(projectId, pipelineId, buildId, channelCode, checkPermission)
    }

    fun goToLatestFinishedBuild(
        userId: String,
        projectId: String,
        pipelineId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): Response {

        if (checkPermission) {
            checkPermission(userId, projectId, pipelineId, BkAuthPermission.VIEW, "用户（$userId) 无权限获取流水线($pipelineId)详情")
        }
        val buildId = pipelineRuntimeService.getLatestFinishedBuildId(pipelineId)
        val apiDomain = HomeHostUtil.innerServerHost()
        val redirectURL = when (buildId) {
            null -> "$apiDomain/console/pipeline/$projectId/$pipelineId/history"
            else -> "$apiDomain/console/pipeline/$projectId/$pipelineId/detail/$buildId"
        }
        val uri = UriBuilder.fromUri(redirectURL).build()
        return Response.temporaryRedirect(uri).build()
    }

    fun getBuildStatus(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): BuildHistory {
        if (checkPermission) {
            checkPermission(
                userId,
                projectId,
                pipelineId,
                BkAuthPermission.VIEW,
                "用户（$userId) 无权限获取流水线($pipelineId)构建状态"
            )
        }

        val buildHistories = pipelineRuntimeService.getBuildHistoryByIds(setOf(buildId))

        if (buildHistories.isEmpty()) {
            throw NotFoundException("构建不存在")
        }
        return buildHistories[0]
    }

    fun getBuildVars(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        checkPermission: Boolean
    ): Result<BuildHistoryVariables> {
        if (checkPermission) {
            checkPermission(
                userId,
                projectId,
                pipelineId,
                BkAuthPermission.VIEW,
                "用户（$userId) 无权限获取流水线($pipelineId)构建变量"
            )
        }

        val buildHistories = pipelineRuntimeService.getBuildHistoryByIds(setOf(buildId))

        if (buildHistories.isEmpty()) {
            return MessageCodeUtil.generateResponseDataObject(ERROR_NO_BUILD_EXISTS_BY_ID.toString(), arrayOf(buildId))
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: return MessageCodeUtil.generateResponseDataObject(
                ERROR_NO_PIPELINE_EXISTS_BY_ID.toString(),
                arrayOf(buildId)
            )

        val allVariable = pipelineRuntimeService.getAllVariable(buildId)

        return Result(
            BuildHistoryVariables(
                id = buildHistories[0].id,
                userId = buildHistories[0].userId,
                trigger = buildHistories[0].trigger,
                pipelineName = pipelineInfo.pipelineName,
                buildNum = buildHistories[0].buildNum ?: 1,
                pipelineVersion = buildHistories[0].pipelineVersion,
                status = buildHistories[0].status,
                startTime = buildHistories[0].startTime,
                endTime = buildHistories[0].endTime,
                variables = allVariable
            )
        )
    }

    fun getBatchBuildStatus(
        projectId: String,
        buildIdSet: Set<String>,
        channelCode: ChannelCode,
        checkPermission: Boolean
    ): List<BuildHistory> {
        val buildHistories = pipelineRuntimeService.getBuildHistoryByIds(buildIdSet)

        if (buildHistories.isEmpty()) {
            return emptyList()
        }
        return buildHistories
    }

    fun getHistoryBuild(
        userId: String?,
        projectId: String,
        pipelineId: String,
        page: Int?,
        pageSize: Int?,
        channelCode: ChannelCode,
        checkPermission: Boolean = true
    ): BuildHistoryPage<BuildHistory> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 1000
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 1000

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, channelCode)
            ?: throw NotFoundException("流水线[$pipelineId]不存在于[$projectId|$channelCode]")

        val apiStartEpoch = System.currentTimeMillis()
        try {
            if (checkPermission) {
                checkPermission(
                    userId!!,
                    projectId,
                    pipelineId,
                    BkAuthPermission.VIEW,
                    "用户（$userId) 无权限获取流水线($pipelineId)历史构建"
                )
            }

            val newTotalCount = pipelineRuntimeService.getPipelineBuildHistoryCount(projectId, pipelineId)
            val newHistoryBuilds = pipelineRuntimeService.listPipelineBuildHistory(projectId, pipelineId, offset, limit)
            val buildHistories = mutableListOf<BuildHistory>()
            buildHistories.addAll(newHistoryBuilds)
            val count = newTotalCount + 0L
            // 获取流水线版本号
            val result = BuildHistoryWithPipelineVersion(
                SQLPage(count, buildHistories),
                if (!checkPermission)
                    true
                else
                    hasDownloadPermission(userId!!, projectId, pipelineId),
                pipelineInfo.version
            )
            return BuildHistoryPage(
                pageNotNull,
                pageSizeNotNull,
                result.history.count,
                result.history.records,
                result.hasDownloadPermission,
                result.pipelineVersion
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
        remark: String?
    ): BuildHistoryPage<BuildHistory> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 1000
        val sqlLimit =
            if (pageSizeNotNull != -1) PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull) else null
        val offset = sqlLimit?.offset ?: 0
        val limit = sqlLimit?.limit ?: 1000

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId, ChannelCode.BS)
            ?: throw NotFoundException("流水线[$pipelineId]不存在于[$projectId|${ChannelCode.BS}]")

        val apiStartEpoch = System.currentTimeMillis()
        try {
            checkPermission(
                userId!!,
                projectId,
                pipelineId,
                BkAuthPermission.VIEW,
                "用户（$userId) 无权限获取流水线($pipelineId)历史构建"
            )

            val newTotalCount = pipelineRuntimeService.getPipelineBuildHistoryCount(
                projectId,
                pipelineId,
                materialAlias,
                materialUrl,
                materialBranch,
                materialCommitId,
                materialCommitMessage,
                status,
                trigger,
                queueTimeStartTime,
                queueTimeEndTime,
                startTimeStartTime,
                startTimeEndTime,
                endTimeStartTime,
                endTimeEndTime,
                totalTimeMin,
                totalTimeMax,
                remark
            )
            val newHistoryBuilds = pipelineRuntimeService.listPipelineBuildHistory(
                projectId,
                pipelineId,
                offset,
                limit,
                materialAlias,
                materialUrl,
                materialBranch,
                materialCommitId,
                materialCommitMessage,
                status,
                trigger,
                queueTimeStartTime,
                queueTimeEndTime,
                startTimeStartTime,
                startTimeEndTime,
                endTimeStartTime,
                endTimeEndTime,
                totalTimeMin,
                totalTimeMax,
                remark
            )
            val buildHistories = mutableListOf<BuildHistory>()
            buildHistories.addAll(newHistoryBuilds)
            val count = newTotalCount + 0L
            // 获取流水线版本号
            val result = BuildHistoryWithPipelineVersion(
                SQLPage(count, buildHistories),
                hasDownloadPermission(userId, projectId, pipelineId),
                pipelineInfo.version
            )
            return BuildHistoryPage(
                pageNotNull,
                pageSizeNotNull,
                result.history.count,
                result.history.records,
                result.hasDownloadPermission,
                result.pipelineVersion
            )
        } finally {
            jmxApi.execute(ProcessJmxApi.LIST_NEW_BUILDS_DETAIL, System.currentTimeMillis() - apiStartEpoch)
        }
    }

    fun updateRemark(userId: String, projectId: String, pipelineId: String, buildId: String, remark: String?) {
        checkPermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.EDIT,
            "用户（$userId) 无权限修改流水线($pipelineId)历史构建"
        )
        pipelineRuntimeService.updateBuildRemark(projectId, pipelineId, buildId, remark)
    }

    fun getHistoryConditionStatus(userId: String, projectId: String, pipelineId: String): List<IdValue> {
        checkPermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.VIEW,
            "用户（$userId) 无权限查看流水线($pipelineId)历史构建"
        )
        return BuildStatus.getStatusMap()
    }

    fun getHistoryConditionTrigger(userId: String, projectId: String, pipelineId: String): List<IdValue> {
        checkPermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.VIEW,
            "用户（$userId) 无权限查看流水线($pipelineId)历史构建"
        )
        return StartType.getStartTypeMap()
    }

    fun getHistoryConditionRepo(userId: String, projectId: String, pipelineId: String): List<String> {
        checkPermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.VIEW,
            "用户（$userId) 无权限查看流水线($pipelineId)历史构建"
        )
        return pipelineRuntimeService.getHistoryConditionRepo(projectId, pipelineId)
    }

    fun getHistoryConditionBranch(
        userId: String,
        projectId: String,
        pipelineId: String,
        alias: List<String>?
    ): List<String> {
        checkPermission(
            userId,
            projectId,
            pipelineId,
            BkAuthPermission.VIEW,
            "用户（$userId) 无权限查看流水线($pipelineId)历史构建"
        )
        return pipelineRuntimeService.getHistoryConditionBranch(projectId, pipelineId, alias)
    }

    fun serviceBuildBasicInfo(buildId: String): BuildBasicInfo {
        val build = pipelineRuntimeService.getBuildInfo(buildId)
            ?: throw NotFoundException("构建不存在")
        return BuildBasicInfo(buildId, build.projectId, build.pipelineId, build.version)
    }

    fun batchServiceBasic(buildIds: Set<String>): Map<String, BuildBasicInfo> {
        val buildBasicInfoMap = pipelineRuntimeService.getBuildBasicInfoByIds(buildIds)
        if (buildBasicInfoMap.isEmpty()) {
            return emptyMap()
        }
        return buildBasicInfoMap
    }

    private fun fillingRuleInOutElement(
        projectId: String,
        pipelineId: String,
        startParams: MutableMap<String, Any>,
        model: Model
    ): Model {
        return model
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
                if (BuildStatus.isFinish(status)) {
                    statusSet.add(status)
                } else if (BuildStatus.isRunning(status)) {
                    statusSet.add(status)
                }
            }
        }
        val buildHistory = pipelineRuntimeService.getBuildHistoryByBuildNum(projectId, pipelineId, buildNum, statusSet)
        logger.info("[$pipelineId]|buildHistory=$buildHistory")
        return buildHistory
    }

    fun getModel(projectId: String, pipelineId: String, version: Int? = null) =
        pipelineRepositoryService.getModel(pipelineId, version) ?: throw NotFoundException("流水线编排不存在")

    private fun buildManualShutdown(
        projectId: String,
        pipelineId: String,
        buildId: String,
        userId: String,
        channelCode: ChannelCode
    ) {

        val redisLock = RedisLock(redisOperation, "process.pipeline.build.shutdown.$buildId", 20)
        try {
            redisLock.lock()

            val modelDetail = buildDetailService.get(buildId)
                ?: return
            val alreadyCancelUser = modelDetail.cancelUserId

            if (!alreadyCancelUser.isNullOrBlank()) {
                logger.warn("The build $buildId of project $projectId already cancel by user $alreadyCancelUser")
                throw OperationException("流水线已经被${alreadyCancelUser}取消构建")
            }

            val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)

            if (pipelineInfo == null) {
                logger.warn("The pipeline($pipelineId) of project($projectId) is not exist")
                return
            }
            if (pipelineInfo.channelCode != channelCode) {
                return
            }

            val buildInfo = pipelineRuntimeService.getBuildInfo(buildId)
            if (buildInfo == null) {
                logger.warn("The build($buildId) of pipeline($pipelineId) is not exist")
                throw NotFoundException("流水线构建不存在")
            } else { /* 父流水线不用在这关闭了，有回调关闭
                if (buildInfo.parentBuildId != null && buildInfo.parentBuildId != buildId) {
                    if (StartType.PIPELINE.name == buildInfo.trigger) {
                        if (buildInfo.parentTaskId != null) {
                            val superPipeline = pipelineRuntimeService.getBuildInfo(buildInfo.parentBuildId)
                            if (superPipeline != null) {
                                logger.info("Shutdown the super build(${superPipeline.buildId}) of pipeline(${superPipeline.pipelineId})")
                                buildManualShutdown(projectId,
                                        superPipeline.pipelineId,
                                        superPipeline.buildId,
                                        userId,
                                        channelCode)
                            }
                        }
                    }
                }*/
            }

            val model = getModel(projectId, pipelineId, buildInfo.version)
            val tasks = getRunningTask(projectId, buildId)
            var isPrepareEnv = true
            model.stages.forEachIndexed { index, stage ->
                if (index == 0) {
                    return@forEachIndexed
                }
                stage.containers.forEach { container ->
                    container.elements.forEach { e ->
                        tasks.forEach { task ->
                            if (task.first == e.id) {
                                isPrepareEnv = false
                                logger.info("Pipeline($pipelineId) build($buildId) shutdown by $userId, elementId: ${task.first}")
                                LogUtils.addYellowLine(rabbitTemplate, buildId, "流水线被用户终止，操作人:$userId", task.first, 1)
                                LogUtils.addFoldEndLine(
                                    rabbitTemplate,
                                    buildId,
                                    "${e.name}-[${task.first}]",
                                    task.first,
                                    1
                                )
                            }
                        }
                    }
                }
            }

            if (isPrepareEnv) {
                LogUtils.addYellowLine(rabbitTemplate, buildId, "流水线被用户终止，操作人:$userId", "", 1)
            }

            try {
                pipelineRuntimeService.cancelBuild(projectId, pipelineId, buildId, userId, BuildStatus.CANCELED)
                buildDetailService.updateBuildCancelUser(buildId, userId)
                logger.info("Cancel the pipeline($pipelineId) of instance($buildId) by the user($userId)")
            } catch (t: Throwable) {
                logger.warn("Fail to shutdown the build($buildId) of pipeline($pipelineId)", t)
            }
        } finally {
            redisLock.unlock()
        }
    }

    private fun getRunningTask(projectId: String, buildId: String): List<Pair<String/*taskId*/, BuildStatus>> {
        return pipelineRuntimeService.getRunningTask(projectId, buildId)
    }

    private fun startPipeline(
        userId: String,
        readyToBuildPipelineInfo: PipelineInfo,
        startType: StartType,
        startParams: MutableMap<String, Any>,
        channelCode: ChannelCode,
        isMobile: Boolean,
        model: Model,
        signPipelineVersion: Int? = null, // 指定的版本
        frequencyLimit: Boolean = true
    ): String {

        val redisLock = RedisLock(redisOperation, "build:limit:${readyToBuildPipelineInfo.pipelineId}", 5L)
        try {
            if (frequencyLimit && channelCode !in NO_LIMIT_CHANNEL && !redisLock.tryLock()) {
                throw OperationException("不能太频繁启动构建")
            }

            // 如果指定了版本号，则设置指定的版本号
            readyToBuildPipelineInfo.version = signPipelineVersion ?: readyToBuildPipelineInfo.version

            val fullModel = model

            val interceptResult = pipelineInterceptorChain.filter(
                InterceptData(readyToBuildPipelineInfo, fullModel, startType)
            )

            if (interceptResult.isNotOk()) {
                // 发送排队失败的事件
                logger.error("[${readyToBuildPipelineInfo.pipelineId}]|START_PIPELINE_$startType|流水线启动失败:[${interceptResult.message}]")
                throw OperationException("流水线启动失败![${interceptResult.message}]")
            }

            val params = startParams.plus(
                mapOf(
                    PIPELINE_VERSION to readyToBuildPipelineInfo.version,
                    PIPELINE_START_USER_ID to userId,
                    PIPELINE_START_TYPE to startType.name,
                    PIPELINE_START_CHANNEL to channelCode.name,
                    PIPELINE_START_MOBILE to isMobile,
                    PIPELINE_NAME to readyToBuildPipelineInfo.pipelineName
                )
            ).plus(
                when (startType) {
                    StartType.PIPELINE -> {
                        mapOf(
                            if (startParams[PIPELINE_START_PIPELINE_USER_ID] != null) {
                                PIPELINE_START_USER_NAME to startParams[PIPELINE_START_PIPELINE_USER_ID]!!
                            } else {
                                PIPELINE_START_USER_NAME to userId
                            }
                        )
                    }
                    StartType.MANUAL -> mapOf(
                        PIPELINE_START_USER_NAME to userId
                    )
                    StartType.WEB_HOOK -> mapOf(
                        if (startParams[PIPELINE_START_WEBHOOK_USER_ID] != null) {
                            PIPELINE_START_USER_NAME to startParams[PIPELINE_START_WEBHOOK_USER_ID]!!
                        } else {
                            PIPELINE_START_USER_NAME to userId
                        }
                    )
                    else -> {
                        mapOf(PIPELINE_START_USER_NAME to userId)
                    }
                }
            )

            val buildId = pipelineRuntimeService.startBuild(readyToBuildPipelineInfo, fullModel, params)
            if (startParams.isNotEmpty()) {
                buildStartupParamService.addParam(buildId, JsonUtil.toJson(startParams))
            }

            logger.info("[${readyToBuildPipelineInfo.pipelineId}]|START_PIPELINE|startType=$startType|startParams=$startParams")

            return buildId
        } finally {
            if (readyToBuildPipelineInfo.channelCode !in NO_LIMIT_CHANNEL) redisLock.unlock()
        }
    }
}
