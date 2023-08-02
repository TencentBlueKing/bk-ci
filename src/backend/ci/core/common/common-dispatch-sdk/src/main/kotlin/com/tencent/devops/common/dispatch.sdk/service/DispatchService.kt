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

package com.tencent.devops.common.dispatch.sdk.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.constant.CommonMessageCode.JOB_BUILD_STOPS
import com.tencent.devops.common.api.constant.CommonMessageCode.UNABLE_GET_PIPELINE_JOB_STATUS
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.pojo.ErrorType
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
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
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
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import java.util.Date
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory

class DispatchService constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val gateway: String?,
    private val client: Client,
    private val channelUtils: ChannelUtils,
    private val buildLogPrinter: BuildLogPrinter,
    private val commonConfig: CommonConfig
) {

    fun log(buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
        )
    }

    fun logRed(buildId: String, containerHashId: String?, vmSeqId: String, message: String, executeCount: Int?) {
        buildLogPrinter.addRedLine(
            buildId,
            message,
            VMUtils.genStartVMTaskId(vmSeqId),
            containerHashId,
            executeCount ?: 1
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
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            buildId = event.buildId,
            dispatchMessage = event.dispatchType.value,
            userId = event.userId,
            vmSeqId = event.vmSeqId,
            channelCode = event.channelCode,
            vmNames = event.vmNames,
            atoms = event.atoms,
            zone = event.zone,
            containerHashId = event.containerHashId,
            executeCount = event.executeCount,
            containerId = event.containerId,
            containerType = event.containerType,
            stageId = event.stageId,
            dispatchType = event.dispatchType,
            customBuildEnv = customBuildEnv,
            dockerRoutingType = event.dockerRoutingType
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
        // 判断流水线当前container是否在运行中
        val statusResult = client.get(ServicePipelineTaskResource::class).getTaskStatus(
            projectId = event.projectId,
            buildId = event.buildId,
            taskId = VMUtils.genStartVMTaskId(event.containerId)
        )

        if (statusResult.isNotOk() || statusResult.data == null) {
            logger.warn("The build event($event) fail to check if pipeline task is running " +
                            "because of ${statusResult.message}")
            val errorMessage = I18nUtil.getCodeLanMessage(UNABLE_GET_PIPELINE_JOB_STATUS)
            throw BuildFailureException(
                errorType = ErrorType.SYSTEM,
                errorCode = UNABLE_GET_PIPELINE_JOB_STATUS.toInt(),
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }

        if (!statusResult.data!!.isRunning()) {
            logger.warn("The build event($event) is not running")
            // dispatch主动发起的重试，当遇到流水线非运行状态时，主动停止消费
            if (event.retryTime > 1) {
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

    fun onContainerFailure(event: PipelineAgentStartupEvent, e: BuildFailureException) {
        logger.warn("[${event.buildId}|${event.vmSeqId}] Container startup failure")
        try {
            client.get(ServiceBuildResource::class).setVMStatus(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                vmSeqId = event.vmSeqId,
                status = BuildStatus.FAILED,
                errorType = e.errorType,
                errorCode = e.errorCode,
                errorMsg = e.formatErrorMessage
            )
        } catch (ignore: ClientException) {
            logger.error("SystemErrorLogMonitor|onContainerFailure|${event.buildId}|error=${e.message},${e.errorCode}")
        }
    }

    fun redispatch(event: IPipelineEvent) {
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
        val redisResult = redisOperation.hget(key = secretInfoRedisKey,
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
                    zone = event.zone,
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
    }
}
