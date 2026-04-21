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

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY
import com.tencent.devops.common.dispatch.sdk.utils.BeanUtil
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupDemoteEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class JobQuotaService constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobQuotaService::class.java)

        // 重试配置常量
        private const val RETRY_TIME = 5
        private const val RETRY_DELTA_MILLIS = 60 * 1000
        private const val RETRY_DELTA_SECONDS = RETRY_DELTA_MILLIS / 1000

        // 默认值常量
        private const val DEFAULT_EXECUTE_COUNT = 1
        private const val DEFAULT_VM_SEQ_ID = "1"
        private const val DEFAULT_QUEUE_TIMEOUT_MINUTES = 10
    }

    @Value("\${dispatch.jobQuota.enable:false}")
    private val jobQuotaEnable: Boolean = false

    fun checkAndAddRunningJob(
        startupEvent: PipelineAgentStartupEvent,
        jobType: JobQuotaVmType?,
        demoteQueueRouteKeySuffix: String
    ): Boolean {
        // 如果未启用配额检查或 jobType 为空，直接通过
        if (!isJobQuotaEnabled(jobType)) {
            return true
        }

        with(startupEvent) {
            val logPrefix = buildLogPrefix(projectId, jobType!!, buildId, vmSeqId, executeCount)

            // 检查并添加运行中的 Job
            val checkResult = checkAndAddRunningJob(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                channelCode = channelCode,
                vmType = jobType
            )

            // 检查通过，返回 true
            if (checkResult == null || checkResult) {
                logger.info("$logPrefix Check job quota success.")
                return true
            }

            // 检查失败，处理配额超限情况
            handleJobQuotaOverrun(
                logPrefix = logPrefix,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                queueTimeoutMinutes = queueTimeoutMinutes ?: DEFAULT_QUEUE_TIMEOUT_MINUTES,
                jobType = jobType,
                demoteQueueRouteKeySuffix = demoteQueueRouteKeySuffix,
                startupEvent = startupEvent
            )

            return false
        }
    }

    /**
     * 处理 Job 配额超限情况
     * 根据重试次数采取不同的处理策略：
     * 1. 前 5 次：普通队列重试
     * 2. 第 5 次：转移到降级队列
     * 3. 之后：在降级队列中继续重试
     * 4. 超过最大重试次数：抛出异常
     */
    fun handleJobQuotaOverrun(
        logPrefix: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        queueTimeoutMinutes: Int,
        jobType: JobQuotaVmType,
        demoteQueueRouteKeySuffix: String,
        startupEvent: IEvent
    ) {
        val maxJobRetry = calculateMaxRetryTimes(queueTimeoutMinutes)
        val dispatchService = SpringContextUtil.getBean(DispatchService::class.java)
        val effectiveExecuteCount = executeCount ?: DEFAULT_EXECUTE_COUNT

        when {
            // 阶段 1: 普通队列重试（前 5 次）
            startupEvent.retryTime < RETRY_TIME -> {
                logger.info("$logPrefix Job quota excess. Delay $RETRY_DELTA_MILLIS ms and retry. " +
                        "RetryTime: ${startupEvent.retryTime}")

                printYellowLog(
                    buildId = buildId,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    executeCount = effectiveExecuteCount,
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY,
                    params = arrayOf(jobType.displayName, "$RETRY_DELTA_SECONDS", "${startupEvent.retryTime}")
                )

                retryEvent(startupEvent, dispatchService)
            }

            // 阶段 2: 转移到降级队列（第 5 次）
            startupEvent.retryTime == RETRY_TIME -> {
                logger.warn("$logPrefix Job quota excess. Transferring event to demote queue.")

                printYellowLog(
                    buildId = buildId,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    executeCount = effectiveExecuteCount,
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED,
                    params = arrayOf(jobType.displayName, "${startupEvent.retryTime}")
                )

                retryEventInDemoteQueue(startupEvent, demoteQueueRouteKeySuffix, dispatchService)
            }

            // 阶段 3: 降级队列中继续重试
            startupEvent.retryTime < maxJobRetry -> {
                logger.info("$logPrefix DemoteQueue job quota excess. Delay $RETRY_DELTA_MILLIS ms and retry. " +
                        "RetryTime: ${startupEvent.retryTime}")

                printYellowLog(
                    buildId = buildId,
                    containerId = containerId,
                    containerHashId = containerHashId,
                    executeCount = effectiveExecuteCount,
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED,
                    params = arrayOf(jobType.displayName, "$RETRY_DELTA_SECONDS", "${startupEvent.retryTime}")
                )

                retryEventInDemoteQueue(startupEvent, demoteQueueRouteKeySuffix, dispatchService)
            }

            // 阶段 4: 超过最大重试次数，抛出异常
            else -> {
                logger.error("$logPrefix Maximum number of retries reached. " +
                        "RetryTime: ${startupEvent.retryTime}, MaxJobRetry: $maxJobRetry")
                throwJobQuotaExcessException(buildId, vmSeqId, executeCount ?: DEFAULT_EXECUTE_COUNT)
            }
        }
    }

    /**
     * 检查并添加运行中的 Job（无代理模式）
     */
    fun checkAndAddRunningJob(
        agentLessStartupEvent: PipelineBuildLessStartupEvent,
        vmType: JobQuotaVmType?
    ): Boolean {
        if (!isJobQuotaEnabled(vmType)) {
            return true
        }

        with(agentLessStartupEvent) {
            return checkAndAddRunningJob(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                channelCode = channelCode,
                vmType = vmType!!
            ) ?: true
        }
    }

    /**
     * 移除运行中的 Job
     */
    fun removeRunningJob(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ) {
        if (!jobQuotaEnable) {
            return
        }

        val effectiveVmSeqId = vmSeqId ?: DEFAULT_VM_SEQ_ID
        val effectiveExecuteCount = executeCount ?: DEFAULT_EXECUTE_COUNT

        logger.info("Remove running job: [$projectId|$buildId|$effectiveVmSeqId|$effectiveExecuteCount]")

        try {
            client.get(ServiceJobQuotaBusinessResource::class).removeRunningJob(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = effectiveVmSeqId,
                executeCount = effectiveExecuteCount
            )
        } catch (e: Throwable) {
            logger.error("Failed to remove running job quota: " +
                    "[$projectId|$buildId|$effectiveVmSeqId|$effectiveExecuteCount]", e)
        }
    }

    /**
     * 检查并添加运行中的 Job（内部方法）
     * @return true-检查通过，false-配额超限，null-检查失败（默认通过）
     */
    private fun checkAndAddRunningJob(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        channelCode: String,
        vmType: JobQuotaVmType
    ): Boolean? {
        val effectiveExecuteCount = executeCount ?: DEFAULT_EXECUTE_COUNT
        val logPrefix = buildLogPrefix(projectId, vmType, buildId, vmSeqId, effectiveExecuteCount)

        logger.info("$logPrefix Start checking job quota.")

        return try {
            client.get(ServiceJobQuotaBusinessResource::class).checkAndAddRunningJob(
                projectId = projectId,
                vmType = vmType,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = effectiveExecuteCount,
                containerId = containerId,
                containerHashId = containerHashId,
                channelCode = channelCode
            ).data
        } catch (e: Throwable) {
            logger.error("$logPrefix Failed to check job quota, allowing by default.", e)
            // 检查失败时默认通过，避免阻塞构建
            true
        }
    }

    /**
     * 转换为降级事件
     */
    private fun transferDemoteEvent(event: IEvent): IEvent {
        return if (event is PipelineAgentStartupEvent) {
            PipelineAgentStartupDemoteEvent(
                source = event.source,
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                pipelineName = event.pipelineName,
                userId = event.userId,
                buildId = event.buildId,
                buildNo = event.buildNo,
                vmSeqId = event.vmSeqId,
                taskName = event.taskName,
                os = event.os,
                vmNames = event.vmNames,
                channelCode = event.channelCode,
                dispatchType = event.dispatchType,
                containerId = event.containerId,
                containerHashId = event.containerHashId,
                queueTimeoutMinutes = event.queueTimeoutMinutes,
                atoms = event.atoms,
                executeCount = event.executeCount,
                customBuildEnv = event.customBuildEnv,
                dockerRoutingType = event.dockerRoutingType,
                routeKeySuffix = event.routeKeySuffix,
                jobId = event.jobId,
                ignoreEnvAgentIds = event.ignoreEnvAgentIds,
                singleNodeConcurrency = event.singleNodeConcurrency,
                allNodeConcurrency = event.allNodeConcurrency,
                dispatchQueueStartTimeMilliSecond = event.dispatchQueueStartTimeMilliSecond,
                actionType = event.actionType,
                delayMills = event.delayMills
            )
        } else {
            event
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查是否启用了 Job 配额功能
     */
    private fun isJobQuotaEnabled(jobType: JobQuotaVmType?): Boolean {
        if (jobType == null || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
            return false
        }
        return true
    }

    /**
     * 构建日志前缀
     */
    private fun buildLogPrefix(
        projectId: String,
        jobType: JobQuotaVmType,
        buildId: String,
        vmSeqId: String,
        executeCount: Int?
    ): String {
        return "$projectId|$jobType|$buildId|$vmSeqId|${executeCount ?: DEFAULT_EXECUTE_COUNT}"
    }

    /**
     * 计算最大重试次数
     */
    private fun calculateMaxRetryTimes(queueTimeoutMinutes: Int): Int {
        return queueTimeoutMinutes * 60 * 1000 / RETRY_DELTA_MILLIS
    }

    /**
     * 打印黄色日志
     */
    private fun printYellowLog(
        buildId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int,
        messageCode: String,
        params: Array<String>
    ) {
        val taskId = VMUtils.genStartVMTaskId(containerId)
        buildLogPrinter.addYellowLine(
            buildId = buildId,
            message = I18nUtil.getCodeLanMessage(
                messageCode = messageCode,
                params = params,
                language = I18nUtil.getDefaultLocaleLanguage()
            ),
            tag = taskId,
            containerHashId = containerHashId,
            executeCount = executeCount,
            jobId = null,
            stepId = taskId
        )
    }

    /**
     * 重试事件（普通队列）
     */
    private fun retryEvent(event: IEvent, dispatchService: DispatchService) {
        event.retryTime += 1
        event.delayMills = RETRY_DELTA_MILLIS
        dispatchService.redispatch(event)
    }

    /**
     * 重试事件（降级队列）
     */
    private fun retryEventInDemoteQueue(
        event: IEvent,
        demoteQueueRouteKeySuffix: String,
        dispatchService: DispatchService
    ) {
        event.retryTime += 1
        event.delayMills = RETRY_DELTA_MILLIS
        if (event is PipelineAgentStartupEvent) {
            event.routeKeySuffix = demoteQueueRouteKeySuffix
        }
        dispatchService.redispatch(transferDemoteEvent(event))
    }

    /**
     * 抛出配额超限异常
     */
    private fun throwJobQuotaExcessException(
        buildId: String,
        vmSeqId: String,
        executeCount: Int
    ): Nothing {
        // 记录配额不足导致失败状态
        BeanUtil.getDispatchMessageTracking().trackQuotaInSufficient(
            buildId = buildId,
            vmSeqId = vmSeqId,
            executeCount = executeCount,
        )

        val errorMessage = I18nUtil.getCodeLanMessage(DispatchSdkErrorCode.JOB_QUOTA_EXCESS.toString())
        throw BuildFailureException(
            errorType = ErrorType.USER,
            errorCode = DispatchSdkErrorCode.JOB_QUOTA_EXCESS,
            formatErrorMessage = errorMessage,
            errorMessage = errorMessage
        )
    }
}
