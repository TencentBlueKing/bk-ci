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

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class JobQuotaService constructor(
    private val client: Client,
    private val buildLogPrinter: BuildLogPrinter
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobQuotaService::class.java)
    }

    @Value("\${dispatch.jobQuota.systemAlertReceiver:#{null}}")
    private val alertReceiver: String? = null

    @Value("\${dispatch.jobQuota.enable}")
    private val jobQuotaEnable: Boolean = false

    fun addRunningJob(projectId: String, vmType: JobQuotaVmType?, buildId: String, vmSeqId: String) {
        if (null == vmType) {
            logger.warn("vmType is null, job quota check will be skipped.")
            return
        }
        logger.info("Add running job to dispatch:[$projectId|$vmType|$buildId|$vmSeqId]")
        try {
            client.get(ServiceJobQuotaBusinessResource::class).addRunningJob(projectId, vmType, buildId, vmSeqId)
        } catch (e: Throwable) {
            logger.error("Add running job quota failed.[$projectId]|[$buildId]|[$vmSeqId]", e)
        }
    }

    fun removeRunningJob(projectId: String, buildId: String, vmSeqId: String?) {
        logger.info("Remove running job to dispatch:[$projectId|$buildId|$vmSeqId]")
        try {
            client.get(ServiceJobQuotaBusinessResource::class).removeRunningJob(projectId, buildId, vmSeqId)
        } catch (e: Throwable) {
            logger.error("Remove running job quota failed.[$projectId]|[$buildId]|[$vmSeqId]", e)
        }
    }

    fun checkJobQuota(startupEvent: PipelineAgentStartupEvent, vmType: JobQuotaVmType?): Boolean {
        if (vmType == null) {
            logger.warn("vmType is null, job quota check will be skipped.")
            return true
        }
        val jobStatus = try {
            client.get(ServiceJobQuotaBusinessResource::class).getRunningJobCount(startupEvent.projectId, vmType).data ?: return true
        } catch (e: Throwable) {
            logger.warn("Get running job count failed.", e)
            return true
        }
        logger.info("Check job quota...")
        with(jobStatus) {
            if (runningJobCount >= jobQuota) {
                logger.warn("Running job count:$runningJobCount, quota: $jobQuota, stop it.(${startupEvent.pipelineId}|${startupEvent.buildId}|${startupEvent.vmSeqId})")
                buildLogPrinter.addRedLine(
                    buildId = startupEvent.buildId,
                    message = "当前项目下正在执行的【${vmType.displayName}】JOB数量已经达到配额最大值，正在执行JOB数量：$runningJobCount, 配额: $jobQuota",
                    tag = VMUtils.genStartVMTaskId(startupEvent.containerId),
                    jobId = startupEvent.containerHashId,
                    executeCount = startupEvent.executeCount ?: 1
                )
                return !jobQuotaEnable
            }

            if (runningJobCount * 100 / jobQuota >= jobThreshold) {
                buildLogPrinter.addYellowLine(
                    buildId = startupEvent.buildId,
                    message = "当前项目下正在执行的【${vmType.displayName}】JOB数量已经超过告警阈值，正在执行JOB数量：$runningJobCount，配额：$jobQuota，" +
                        "告警阈值：${normalizePercentage(jobThreshold.toDouble())}%，当前已经使用：${normalizePercentage(runningJobCount * 100.0 / jobQuota)}%",
                    tag = VMUtils.genStartVMTaskId(startupEvent.containerId),
                    jobId = startupEvent.containerHashId,
                    executeCount = startupEvent.executeCount ?: 1
                )
            }

            if (runningJobTime >= timeQuota * 60 * 60 * 1000) {
                logger.warn("Running job total time:$runningJobTime(s), quota: $timeQuota(h), stop it.(${startupEvent.pipelineId}|$startupEvent.buildId|${startupEvent.vmSeqId})")
                buildLogPrinter.addRedLine(
                    buildId = startupEvent.buildId,
                    message = "当前项目下本月已执行的【${vmType.displayName}】JOB时间达到配额最大值，已执行JOB时间：${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时, 配额: ${timeQuota}小时",
                    tag = VMUtils.genStartVMTaskId(startupEvent.containerId),
                    jobId = startupEvent.containerHashId,
                    executeCount = startupEvent.executeCount ?: 1
                )
                return !jobQuotaEnable
            }

            if ((runningJobTime * 100) / (timeQuota * 60 * 60 * 1000) >= timeThreshold) {
                buildLogPrinter.addYellowLine(
                    buildId = startupEvent.buildId,
                    message = "前项目下本月已执行的【${vmType.displayName}】JOB时间已经超过告警阈值，已执行JOB时间：${String.format("%.2f", runningJobTime / 1000.0 / 60 / 60)}小时, 配额: ${timeQuota}小时，" +
                        "告警阈值：${normalizePercentage(timeThreshold.toDouble())}%，当前已经使用：${normalizePercentage((runningJobTime * 100.0) / (timeQuota * 60 * 60 * 1000))}%",
                    tag = VMUtils.genStartVMTaskId(startupEvent.containerId),
                    jobId = startupEvent.containerHashId,
                    executeCount = startupEvent.executeCount ?: 1
                )
            }
            logger.info("Check job quota finish.")
            return true
        }
    }

    private fun normalizePercentage(value: Double): String {
        return when {
            value >= 100.0 -> {
                "100.00"
            }
            value <= 0 -> {
                "0.00"
            }
            else -> {
                String.format("%.2f", value)
            }
        }
    }
}