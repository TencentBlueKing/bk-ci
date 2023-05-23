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

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerConstants.BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
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
        vmType: JobQuotaVmType?,
        demoteQueueRouteKeySuffix: String
    ): Boolean {
        if (null == vmType || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
            return true
        }

        val dispatchService = SpringContextUtil.getBean(DispatchService::class.java)

        with(startupEvent) {
            val checkResult = checkAndAddRunningJob(
                projectId = projectId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                containerId = containerId,
                containerHashId = containerHashId,
                executeCount = executeCount,
                vmType = vmType
            )

            if (checkResult == null || checkResult) {
                logger.info("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Check job quota success.")
                return true
            }

            if (startupEvent.retryTime < RETRY_TIME) {
                logger.info("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Job quota excess. delay: " +
                                "$RETRY_DELTA and retry. retryTime: ${startupEvent.retryTime}")

                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY,
                        params = arrayOf(vmType.displayName, "${RETRY_DELTA / 1000}", "${startupEvent.retryTime}"),
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )

                startupEvent.retryTime += 1
                startupEvent.delayMills = RETRY_DELTA
                dispatchService.redispatch(startupEvent)

                return false
            } else if (startupEvent.retryTime == RETRY_TIME) { // 重试次数刚刚超过最大重试次数，会将消息丢到降级队列
                logger.warn(
                    "$projectId|$vmType|$buildId|$vmSeqId|$executeCount " +
                        "Job quota excess. Send event to demoteQueue."
                )

                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED,
                        params = arrayOf(vmType.displayName, "$retryTime")
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )

                startupEvent.retryTime += 1
                startupEvent.delayMills = RETRY_DELTA
                startupEvent.routeKeySuffix = demoteQueueRouteKeySuffix
                dispatchService.redispatch(startupEvent)

                return false
            } else {
                logger.info("$projectId|$vmType|$buildId|$vmSeqId|$executeCount DemoteQueue job quota excess. delay: " +
                                "$RETRY_DELTA and retry. retryTime: ${startupEvent.retryTime}")

                buildLogPrinter.addYellowLine(
                    buildId = buildId,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED,
                        params = arrayOf(vmType.displayName, "${RETRY_DELTA / 1000}", "${startupEvent.retryTime}")
                    ),
                    tag = VMUtils.genStartVMTaskId(containerId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )

                startupEvent.retryTime += 1
                startupEvent.delayMills = RETRY_DELTA
                startupEvent.routeKeySuffix = demoteQueueRouteKeySuffix
                dispatchService.redispatch(startupEvent)

                return false
            }
        }
    }

    fun checkAndAddRunningJob(
        agentLessStartupEvent: PipelineBuildLessStartupDispatchEvent,
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
                containerHashId = containerHashId
            ).data
        } catch (e: Throwable) {
            logger.error("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Check job quota failed.", e)
            true
        }
    }
}
