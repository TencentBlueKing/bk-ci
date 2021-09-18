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

import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.DispatchSdkErrorCode
import com.tencent.devops.dispatch.api.ServiceJobQuotaBusinessResource
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import com.tencent.devops.process.pojo.mq.PipelineBuildLessStartupDispatchEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class JobQuotaService constructor(
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JobQuotaService::class.java)
    }

    @Value("\${dispatch.jobQuota.enable:false}")
    private val jobQuotaEnable: Boolean = false

    fun checkAndAddRunningJob(startupEvent: PipelineAgentStartupEvent, vmType: JobQuotaVmType?) {
        if (null == vmType || !jobQuotaEnable) {
            logger.info("JobQuota not enabled or VmType is null, job quota check will be skipped.")
            return
        }

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

            if (checkResult != null && !checkResult) {
                logger.error("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Job quota excess.")
                throw BuildFailureException(
                    errorType = ErrorType.USER,
                    errorCode = DispatchSdkErrorCode.JOB_QUOTA_EXCESS,
                    formatErrorMessage = "JOB配额超限",
                    errorMessage = "JOB配额超限"
                )
            }

            logger.info("$projectId|$vmType|$buildId|$vmSeqId|$executeCount Check job quota success.")
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

    fun removeRunningJob(projectId: String, buildId: String, vmSeqId: String?, executeCount: Int?) {
        if (jobQuotaEnable) {
            logger.info("Remove running job to dispatch:[$projectId|$buildId|$vmSeqId]")
            try {
                client.get(ServiceJobQuotaBusinessResource::class).removeRunningJob(
                    projectId = projectId,
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
