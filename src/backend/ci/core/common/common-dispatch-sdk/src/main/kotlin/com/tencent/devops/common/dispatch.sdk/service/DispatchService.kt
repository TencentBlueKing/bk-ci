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

package com.tencent.devops.common.dispatch.sdk.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.JOB_BUILD_STOPS
import com.tencent.devops.common.api.constant.CommonMessageCode.UNABLE_GET_PIPELINE_JOB_STATUS
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.RedisBuild
import com.tencent.devops.common.dispatch.sdk.pojo.SecretInfo
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_DEVOPS_FILE_GATEWAY
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_DEVOPS_GATEWAY
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_KEY_AGENT_ID
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_KEY_BUILD_ID
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.ENV_KEY_PROJECT_ID
import com.tencent.devops.common.dispatch.sdk.utils.ChannelUtils
import com.tencent.devops.common.dispatch.sdk.utils.DispatchLogRedisUtils
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.monitoring.api.service.DispatchReportResource
import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineTaskResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.engine.pojo.PipelineBuildContainer
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import feign.RetryableException
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "TooManyFunctions")
class DispatchService constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val pipelineEventDispatcher: SampleEventDispatcher,
    private val gateway: String?,
    private val client: Client,
    private val channelUtils: ChannelUtils,
    private val buildLogPrinter: BuildLogPrinter,
    private val commonConfig: CommonConfig
) {

    fun log(
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?,
        jobId: String
    ) {
        buildLogPrinter.addLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId = containerHashId,
            executeCount = executeCount ?: 1,
            jobId = jobId,
            stepId = VMUtils.genStartVMTaskId(vmSeqId)
        )
    }

    fun logRed(
        buildId: String,
        containerHashId: String?,
        vmSeqId: String,
        message: String,
        executeCount: Int?,
        jobId: String?
    ) {
        buildLogPrinter.addRedLine(
            buildId = buildId,
            message = message,
            tag = VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId = containerHashId,
            executeCount = executeCount ?: 1,
            jobId = jobId,
            stepId = VMUtils.genStartVMTaskId(vmSeqId)
        )
    }

    fun buildDispatchMessage(event: PipelineAgentStartupEvent): DispatchMessage {
        logger.info("[${event.buildId}] Start build with gateway - ($gateway)")
        val secretInfo = setRedisAuth(event)

        val customBuildEnv = event.customBuildEnv?.toMutableMap() ?: mutableMapOf()
        customBuildEnv[ENV_KEY_BUILD_ID] = event.buildId
        customBuildEnv[ENV_KEY_PROJECT_ID] = event.projectId
        customBuildEnv[ENV_KEY_AGENT_ID] = secretInfo.hashId
        customBuildEnv[ENV_KEY_AGENT_SECRET_KEY] = secretInfo.secretKey
        commonConfig.fileDevnetGateway?.let {
            customBuildEnv[ENV_DEVOPS_FILE_GATEWAY] = it
        }
        commonConfig.devopsDevnetProxyGateway?.let {
            customBuildEnv[ENV_DEVOPS_GATEWAY] = it
        }

        return DispatchMessage(
            id = secretInfo.hashId,
            secretKey = secretInfo.secretKey,
            gateway = gateway!!,
            customBuildEnv = customBuildEnv,
            event = event
        )
    }

    fun shutdown(event: PipelineAgentShutdownEvent) {
        val secretInfoKey = secretInfoRedisKey(event.buildId)

        // job结束
        finishBuild(event.vmSeqId!!, event.buildId, event.executeCount ?: 1)
        redisOperation.hdelete(secretInfoKey, secretInfoRedisMapKey(event.vmSeqId!!, event.executeCount ?: 1))
        // 当hash表为空时，redis会自动删除
    }

    fun checkRunning(event: PipelineAgentStartupEvent): Boolean {
        return checkRunning(
            projectId = event.projectId,
            buildId = event.buildId,
            containerId = event.containerId,
            retryTime = event.retryTime,
            executeCount = event.executeCount,
            logTag = "$event"
        )
    }

    fun checkRunning(
        projectId: String,
        buildId: String,
        containerId: String,
        retryTime: Int,
        executeCount: Int?,
        logTag: String?
    ): Boolean {
        val (startBuildTask, buildContainer) = getContainerStartupInfoWithRetry(
            projectId = projectId,
            buildId = buildId,
            containerId = containerId,
            logTag = logTag
        )

        var needStart = true
        if (executeCount != startBuildTask.executeCount) {
            // 如果已经重试过或执行次数不匹配则直接丢弃
            needStart = false
        } else if (startBuildTask.status.isFinish() && buildContainer.status.isRunning()) {
            // 如果Job已经启动在运行或则直接丢弃
            needStart = false
        } else if (!buildContainer.status.isRunning() && !buildContainer.status.isReadyToRun()) {
            needStart = false
        }

        if (!needStart) {
            logger.warn("The build event($logTag) is not running")
            // dispatch主动发起的重试或者用户已取消的流水线忽略异常报错
            if (retryTime > 1 || buildContainer.status.isCancel()) {
                return false
            }

            val errorMessage = I18nUtil.getCodeLanMessage(JOB_BUILD_STOPS)
            throw BuildFailureException(
                errorType = ErrorType.USER,
                errorCode = JOB_BUILD_STOPS.toInt(),
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }
        return true
    }

    fun onFailure(
        event: PipelineAgentStartupEvent,
        e: BuildFailureException
    ) {
        onFailure(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            e = e,
            logTag = "$event"
        )
    }

    fun onFailure(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        e: BuildFailureException,
        logTag: String?
    ) {
        onContainerFailure(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            e = e,
            logTag
        )
        DispatchLogRedisUtils.removeRedisExecuteCount(buildId)
    }

    private fun onContainerFailure(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        e: BuildFailureException,
        logTag: String?
    ) {
        logger.warn("[$buildId|$vmSeqId] Container startup failure")
        try {
            val (startBuildTask, buildContainer) = getContainerStartupInfoWithRetry(
                projectId = projectId,
                buildId = buildId,
                containerId = vmSeqId,
                logTag = logTag
            )
            if (buildContainer.status.isCancel() || startBuildTask.status.isCancel()) {
                return
            }

            client.get(ServiceBuildResource::class).setVMStatus(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                status = BuildStatus.FAILED,
                errorType = e.errorType,
                errorCode = e.errorCode,
                errorMsg = e.formatErrorMessage
            )
        } catch (ignore: ClientException) {
            logger.error("SystemErrorLogMonitor|onContainerFailure|$buildId|error=${e.message},${e.errorCode}")
        }
    }

    fun redispatch(event: IEvent) {
        logger.info("Re-dispatch the agent event - ($event)")
        pipelineEventDispatcher.dispatch(event)
    }

    fun sendDispatchMonitoring(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        actionType: String,
        retryTime: Int,
        routeKeySuffix: String?,
        startTime: Long,
        stopTime: Long,
        errorCode: Int,
        errorType: ErrorType?,
        errorMessage: String?
    ) {
        try {
            client.get(DispatchReportResource::class).dispatch(
                DispatchStatus(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    actionType = actionType,
                    retryCount = retryTime.toLong(),
                    channelCode = channelUtils.getChannelCode(),
                    buildType = routeKeySuffix!!,
                    startTime = startTime,
                    stopTime = stopTime,
                    errorCode = errorCode.toString(),
                    errorMsg = errorMessage,
                    errorType = errorType?.name ?: ""
                )
            )
        } catch (e: Exception) {
            logger.warn("[$pipelineId]|[$buildId]|[$vmSeqId]| sendDispatchMonitoring failed.", e.message)
        }
    }

    /**
     * 针对服务间调用出现 Connection refused 的异常，进行重试
     */
    private fun getContainerStartupInfoWithRetry(
        projectId: String,
        buildId: String,
        containerId: String,
        logTag: String?,
        retryTimes: Int = RETRY_TIMES
    ): Pair<PipelineBuildTask, PipelineBuildContainer> {
        try {
            return getContainerStartupInfo(
                projectId = projectId,
                buildId = buildId,
                containerId = containerId,
                logTag = logTag
            )
        } catch (e: RetryableException) {
            if (retryTimes > 0) {
                logger.warn("[$buildId]|[$containerId]| getContainerStartupInfo failed, " +
                        "retryTimes=$retryTimes", e.message)
                Thread.sleep(1000)
                return getContainerStartupInfoWithRetry(
                    projectId = projectId,
                    buildId = buildId,
                    containerId = containerId,
                    logTag = logTag,
                    retryTimes = retryTimes - 1
                )
            } else {
                throw e
            }
        }
    }

    private fun getContainerStartupInfo(
        projectId: String,
        buildId: String,
        containerId: String,
        logTag: String?
    ): Pair<PipelineBuildTask, PipelineBuildContainer> {
        // 判断流水线当前container是否在运行中
        val statusResult = client.get(ServicePipelineTaskResource::class).getContainerStartupInfo(
            projectId = projectId,
            buildId = buildId,
            containerId = containerId,
            taskId = VMUtils.genStartVMTaskId(containerId)
        )
        val startBuildTask = statusResult.data?.startBuildTask
        val buildContainer = statusResult.data?.buildContainer
        if (statusResult.isNotOk() || startBuildTask == null || buildContainer == null) {
            logger.warn(
                "The build event($logTag) fail to check if pipeline task is running " +
                        "because of statusResult(${statusResult.message})"
            )
            val errorMessage = I18nUtil.getCodeLanMessage(UNABLE_GET_PIPELINE_JOB_STATUS)
            throw BuildFailureException(
                errorType = ErrorType.SYSTEM,
                errorCode = UNABLE_GET_PIPELINE_JOB_STATUS.toInt(),
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }

        return Pair(startBuildTask, buildContainer)
    }

    private fun finishBuild(vmSeqId: String, buildId: String, executeCount: Int) {
        val result = redisOperation.hget(secretInfoRedisKey(buildId), secretInfoRedisMapKey(vmSeqId, executeCount))
        if (result != null) {
            val secretInfo = JsonUtil.to(result, SecretInfo::class.java)
            redisOperation.delete(redisKey(secretInfo.hashId, secretInfo.secretKey))
            logger.warn("$buildId|$vmSeqId finishBuild success.")
        } else {
            logger.warn("$buildId|$vmSeqId finishBuild failed, secretInfo is null.")
        }
    }

    private fun setRedisAuth(event: PipelineAgentStartupEvent): SecretInfo {
        val secretInfoRedisKey = secretInfoRedisKey(event.buildId)
        val redisResult = redisOperation.hget(
            key = secretInfoRedisKey,
            hashKey = secretInfoRedisMapKey(event.vmSeqId, event.executeCount ?: 1)
        )
        if (redisResult != null) {
            return JsonUtil.to(redisResult, SecretInfo::class.java)
        }
        val secretKey = ApiUtil.randomSecretKey()
        val hashId = HashUtil.encodeLongId(System.currentTimeMillis())
        logger.info("[${event.buildId}|${event.vmSeqId}] Start to build the event with ($hashId|$secretKey)")
        redisOperation.set(
            key = redisKey(hashId, secretKey),
            value = objectMapper.writeValueAsString(
                RedisBuild(
                    vmName = event.vmNames.ifBlank { "Dispatcher-sdk-${event.vmSeqId}" },
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    channelCode = event.channelCode,
                    // 待废弃属性
                    zone = Zone.SHENZHEN,
                    atoms = event.atoms,
                    executeCount = event.executeCount ?: 1
                )
            ),
            expiredInSecond = TimeUnit.DAYS.toSeconds(7)
        )

        // 一周过期时间
        redisOperation.hset(
            secretInfoRedisKey(event.buildId),
            secretInfoRedisMapKey(event.vmSeqId, event.executeCount ?: 1),
            JsonUtil.toJson(SecretInfo(hashId, secretKey))
        )
        val expireAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
        redisOperation.expireAt(secretInfoRedisKey, Date(expireAt))
        return SecretInfo(
            hashId = hashId,
            secretKey = secretKey
        )
    }

    private fun redisKey(hashId: String, secretKey: String) =
        "docker_build_key_${hashId}_$secretKey"

    private fun secretInfoRedisKey(buildId: String) =
        "secret_info_key_$buildId"

    private fun secretInfoRedisMapKey(vmSeqId: String, executeCount: Int) = "$vmSeqId-$executeCount"

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchService::class.java)

        private const val RETRY_TIMES = 3
    }
}
