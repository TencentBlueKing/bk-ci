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
        private const val RETRY_TIME = 5
        private const val RETRY_DELTA = 60 * 1000
    }

    @Value("\${dispatch.jobQuota.enable:false}")
    private val jobQuotaEnable: Boolean = false

    fun checkAndAddRunningJob(
        startupEvent: PipelineAgentStartupEvent,
        jobType: JobQuotaVmType?,
        demoteQueueRouteKeySuffix: String
    ): Boolean {
        if (null == jobType || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
            return true
        }

        with(startupEvent) {
            val logPrefix = "$projectId|$jobType|$buildId|$vmSeqId|$executeCount"
            val checkResult = checkAndAddRunningJob(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                channelCode = startupEvent.channelCode,
                vmType = jobType
            )

            if (checkResult == null || checkResult) {
                logger.info("$logPrefix Check job quota success.")
                return true
            }

            jobQuoteOverrunHandler(
                logPrefix = logPrefix,
                buildId = buildId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                queueTimeoutMinutes = queueTimeoutMinutes ?: 10,
                jobType = jobType,
                demoteQueueRouteKeySuffix = demoteQueueRouteKeySuffix,
                startupEvent = startupEvent
            )

            return false
        }
    }

    fun jobQuoteOverrunHandler(
        logPrefix: String,
        buildId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        queueTimeoutMinutes: Int,
        jobType: JobQuotaVmType,
        demoteQueueRouteKeySuffix: String,
        startupEvent: IEvent
    ) {
        val maxJobRetry = queueTimeoutMinutes * 60 * 1000 / RETRY_DELTA
        val dispatchService = SpringContextUtil.getBean(DispatchService::class.java)

        if (startupEvent.retryTime < RETRY_TIME) {
            logger.info("$logPrefix Job quota excess. delay: " +
                    "$RETRY_DELTA and retry. retryTime: ${startupEvent.retryTime}")

            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY,
                    params = arrayOf(jobType.displayName, "${RETRY_DELTA / 1000}", "${startupEvent.retryTime}"),
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                tag = VMUtils.genStartVMTaskId(containerId),
                containerHashId = containerHashId,
                executeCount = executeCount ?: 1,
                jobId = null,
                stepId = VMUtils.genStartVMTaskId(containerId)
            )

            startupEvent.retryTime += 1
            startupEvent.delayMills = RETRY_DELTA
            dispatchService.redispatch(startupEvent)
        } else if (startupEvent.retryTime == RETRY_TIME) { // 重试次数刚刚超过最大重试次数，会将消息丢到降级队列
            logger.warn("$logPrefix Job quota excess. Send event to demoteQueue.")

            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED,
                    params = arrayOf(jobType.displayName, "${startupEvent.retryTime}")
                ),
                tag = VMUtils.genStartVMTaskId(containerId),
                containerHashId = containerHashId,
                executeCount = executeCount ?: 1,
                jobId = null,
                stepId = VMUtils.genStartVMTaskId(containerId)
            )

            startupEvent.retryTime += 1
            startupEvent.delayMills = RETRY_DELTA
            if (startupEvent is PipelineAgentStartupEvent) {
                startupEvent.routeKeySuffix = demoteQueueRouteKeySuffix
            }
            dispatchService.redispatch(transferDemoteEvent(startupEvent))
        } else if (startupEvent.retryTime < maxJobRetry) {
            logger.info("$logPrefix DemoteQueue job quota excess. delay: " +
                    "$RETRY_DELTA and retry. retryTime: ${startupEvent.retryTime}")

            buildLogPrinter.addYellowLine(
                buildId = buildId,
                message = I18nUtil.getCodeLanMessage(
                    messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED,
                    params = arrayOf(jobType.displayName, "${RETRY_DELTA / 1000}", "${startupEvent.retryTime}")
                ),
                tag = VMUtils.genStartVMTaskId(containerId),
                containerHashId = containerHashId,
                executeCount = executeCount ?: 1,
                jobId = null,
                stepId = VMUtils.genStartVMTaskId(containerId)
            )

            startupEvent.retryTime += 1
            startupEvent.delayMills = RETRY_DELTA
            if (startupEvent is PipelineAgentStartupEvent) {
                startupEvent.routeKeySuffix = demoteQueueRouteKeySuffix
            }
            dispatchService.redispatch(transferDemoteEvent(startupEvent))
        } else {
            logger.info("$logPrefix DemoteQueue Job Maximum number of retries reached. " +
                    "RetryTime: ${startupEvent.retryTime}, MaxJobRetry: $maxJobRetry")
            val errorMessage = I18nUtil.getCodeLanMessage(DispatchSdkErrorCode.JOB_QUOTA_EXCESS.toString())
            throw BuildFailureException(
                errorType = ErrorType.USER,
                errorCode = DispatchSdkErrorCode.JOB_QUOTA_EXCESS,
                formatErrorMessage = errorMessage,
                errorMessage = errorMessage
            )
        }
    }

    fun checkAndAddRunningJob(
        agentLessStartupEvent: PipelineBuildLessStartupEvent,
        vmType: JobQuotaVmType?
    ): Boolean {
        if (null == vmType || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
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
                channelCode = agentLessStartupEvent.channelCode,
                vmType = vmType
            ) ?: true
        }
    }

    fun removeRunningJob(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String?,
        executeCount: Int?
    ) {
        if (jobQuotaEnable) {
            logger.info("Remove running job to dispatch:[$projectId|$buildId|$vmSeqId]")
            try {
                client.get(ServiceJobQuotaBusinessResource::class).removeRunningJob(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId ?: "1",
                    executeCount = executeCount ?: 1
                )
            } catch (e: Throwable) {
                logger.error("Remove running job quota failed.[$projectId]|[$buildId]|[$vmSeqId]", e)
            }
        }
    }

    private fun checkAndAddRunningJob(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        containerHashId: String?,
        executeCount: Int?,
        channelCode: String,
        vmType: JobQuotaVmType?
    ): Boolean? {
        if (null == vmType || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
            return true
        }

        logger.info("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Start check job quota.")
        return try {
            client.get(ServiceJobQuotaBusinessResource::class).checkAndAddRunningJob(
                projectId = projectId,
                vmType = vmType,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1,
                containerId = containerId,
                containerHashId = containerHashId,
                channelCode = channelCode
            ).data
        } catch (e: Throwable) {
            logger.error("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Check job quota failed.", e)
            true
        }
    }

    private fun transferDemoteEvent(event: IEvent): IEvent {
        if (event is PipelineAgentStartupEvent) {
            return PipelineAgentStartupDemoteEvent(
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
            return event
        }
    }
}
