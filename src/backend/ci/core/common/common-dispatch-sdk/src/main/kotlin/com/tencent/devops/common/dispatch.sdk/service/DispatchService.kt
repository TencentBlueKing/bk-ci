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

package com.tencent.devops.common.dispatch.sdk.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.SecretInfo
import com.tencent.devops.monitoring.api.service.DispatchReportResource
import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildContainerEvent
import org.slf4j.LoggerFactory
import java.util.Date

class DispatchService constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val gateway: String?,
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
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

    fun build(event: PipelineAgentStartupEvent, startupQueue: String): DispatchMessage {
        logger.info("[${event.buildId}] Start build with gateway - ($gateway)")
        val secretInfo = setRedisAuth(event)
        setStartup(startupQueue, event)
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
            dispatchType = event.dispatchType
        )
    }

    fun getExecuteCount(startupQueue: String): Long {
        return redisOperation.get(executeCountKey(startupQueue))?.toLong() ?: 0
    }

    fun shutdown(event: PipelineAgentShutdownEvent, startupQueue: String) {
        setShutdown(startupQueue)
        val secretInfoKey = secretInfoRedisKey(event.buildId)
        val keysSet = redisOperation.hkeys(secretInfoKey)
        if (keysSet != null && keysSet.isNotEmpty()) {
            if (event.vmSeqId == null) {
                // 流水线结束
                keysSet.forEach {
                    finishBuild(it, event.buildId)
                }
                redisOperation.delete(secretInfoKey)
            } else {
                // job结束
                finishBuild(event.vmSeqId!!, event.buildId)
                redisOperation.hdelete(secretInfoKey, event.vmSeqId!!)
            }
        }
    }

    fun isRunning(event: PipelineAgentStartupEvent) {
        // 判断流水线是否还在运行，如果已经停止则不在运行
        // 只有detail的信息是在shutdown事件发出之前就写入的，所以这里去builddetail的信息。
        // 为了兼容gitci的权限，这里把渠道号都改成GIT,以便去掉用户权限验证
        val record = client.get(ServiceBuildResource::class).getBuildDetailStatusWithoutPermission(
            event.userId,
            event.projectId,
            event.pipelineId,
            event.buildId,
            ChannelCode.BS
        )
        if (record.isNotOk() || record.data == null) {
            logger.warn("The build event($event) fail to check if pipeline is running because of ${record.message}")
            throw BuildFailureException(ErrorType.SYSTEM, DispatchSdkErrorCode.PIPELINE_STATUS_ERROR, "无法获取流水线状态", "无法获取流水线状态")
        }
        val status = BuildStatus.parse(record.data)
        if (!BuildStatus.isRunning(status)) {
            logger.warn("The build event($event) is not running")
            throw BuildFailureException(ErrorType.SYSTEM, DispatchSdkErrorCode.PIPELINE_NOT_RUNNING, "流水线已经不再运行", "流水线已经不再运行")
        }
    }

    fun onContainerFailure(event: PipelineAgentStartupEvent, e: BuildFailureException) {
        logger.warn("[${event.buildId}|${event.vmSeqId}] Container startup failure")
        pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                source = "container_startup_sdk",
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                buildId = event.buildId,
                userId = event.userId,
                stageId = event.stageId,
                containerId = event.containerId,
                containerType = event.containerType,
                actionType = ActionType.TERMINATE
            )
        )

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
    }

    fun redispatch(event: PipelineAgentStartupEvent) {
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
                    channelCode = ChannelCode.BS,
                    buildType = routeKeySuffix!!,
                    startTime = startTime,
                    stopTime = stopTime,
                    errorCode = errorCode.toString(),
                    errorMsg = errorMessage
                )
            )
        } catch (e: Exception) {
            logger.error("[$pipelineId]|[$buildId]|[$vmSeqId]| sendDispatchMonitoring failed.", e)
        }
    }

    private fun finishBuild(vmSeqId: String, buildId: String) {
        val result = redisOperation.hget(secretInfoRedisKey(buildId), vmSeqId)
        if (result != null) {
            val secretInfo = JsonUtil.to(result, SecretInfo::class.java)
            redisOperation.delete(redisKey(secretInfo.hashId, secretInfo.secretKey))
            logger.error("$buildId|$vmSeqId finishBuild success.")
        } else {
            logger.error("$buildId|$vmSeqId finishBuild failed, secretInfo is null.")
        }
    }

    private fun setShutdown(startupQueue: String) {
        try {
            redisOperation.increment(executeCountKey(startupQueue), -1)
        } catch (t: Throwable) {
            logger.warn("Fail to set the shutdown count in redis - $startupQueue", t)
        }
    }

    private fun setRedisAuth(event: PipelineAgentStartupEvent): SecretInfo {
        val redisKey = secretInfoRedisKey(event.buildId)
        val redisResult = redisOperation.hget(redisKey, event.vmSeqId)
        if (redisResult != null) {
            return JsonUtil.to(redisResult, SecretInfo::class.java)
        }
        val secretKey = ApiUtil.randomSecretKey()
        val hashId = HashUtil.encodeLongId(System.currentTimeMillis())
        logger.info("[${event.buildId}|${event.vmSeqId}] Start to build the event with ($hashId|$secretKey)")
        redisOperation.set(
            redisKey(hashId, secretKey),
            objectMapper.writeValueAsString(
                RedisBuild(
                    vmName = if (event.vmNames.isBlank()) "Dispatcher-sdk-${event.vmSeqId}" else event.vmNames,
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    channelCode = event.channelCode,
                    zone = event.zone,
                    atoms = event.atoms
                )
            )
        )

        // 一周过期时间
        redisOperation.hset(
            secretInfoRedisKey(event.buildId),
            event.vmSeqId,
            JsonUtil.toJson(SecretInfo(hashId, secretKey))
        )
        val expireAt = System.currentTimeMillis() + 24 * 7 * 3600
        redisOperation.expireAt(redisKey, Date(expireAt))
        return SecretInfo(
            hashId = hashId,
            secretKey = secretKey
        )
    }

    private fun setStartup(
        startupQueue: String,
        event: PipelineAgentStartupEvent
    ) {
        try {
            if (event.retryTime == 1) {
                redisOperation.increment(executeCountKey(startupQueue), 1)
            }
        } catch (t: Throwable) {
            logger.warn("Fail ot set the start up count in redis - $startupQueue", t)
        }
    }

    private fun redisKey(hashId: String, secretKey: String) =
        "docker_build_key_${hashId}_$secretKey"

    private fun secretInfoRedisKey(buildId: String) =
        "secret_info_key_$buildId"

    private fun executeCountKey(startupQueue: String) =
        "dispatcher:sdk:execute:count:key:$startupQueue"

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchService::class.java)
    }
}