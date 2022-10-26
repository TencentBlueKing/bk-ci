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

package com.tencent.devops.worker.common.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.engine.EngineBuildSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import org.slf4j.LoggerFactory

object EngineService {

    private val logger = LoggerFactory.getLogger(EngineService::class.java)

    private val buildApi = ApiFactory.create(EngineBuildSDKApi::class)

    private const val retryTime = 100

    private const val retryPeriodMills = 5000L

    fun setStarted(): BuildVariables {

        var retryCount = 0
        val result = HttpRetryUtils.retry(retryTime = retryTime) {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|setStarted")
                sleepInterval(retryCount)
            }
            buildApi.setStarted(retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Report builder startup status failed")
        }
        val ret = result.data ?: throw RemoteServiceException("Report builder startup status failed")

        // #5277 将Job上下文传入本次agent任务
        val jobContext = buildApi.getJobContext().toMutableMap()
        jobContext[JOB_OS_CONTEXT] = AgentEnv.getOS().name
        return ret.copy(variables = ret.variables.plus(jobContext))
    }

    fun claimTask(): BuildTask {
        var retryCount = 0
        val result = HttpRetryUtils.retry(retryTime = retryTime) {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|claimTask")
                sleepInterval(retryCount)
            }
            buildApi.claimTask(retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to get build task")
        }
        return result.data ?: throw RemoteServiceException("Failed to get build task")
    }

    fun completeTask(taskResult: BuildTaskResult) {
        LoggerService.flush()
        var retryCount = 0
        val result = HttpRetryUtils.retry(retryTime = retryTime) {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|completeTask")
                sleepInterval(retryCount)
            }
            buildApi.completeTask(taskResult, retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to complete build task")
        }
    }

    private fun sleepInterval(retryCount: Int) {
        if (retryCount > 0) {
            val time = (retryCount % 11) * 1000L // #5109 重试间隔优化，递增最多10秒
            logger.warn("sleepInterval|retryCount=$retryCount|sleepTime=$time")
            Thread.sleep(time)
        }
    }

    fun endBuild(variables: Map<String, String>, buildId: String = "") {
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|endBuild")
                sleepInterval(retryCount)
            }
            buildApi.endTask(variables, buildId, retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to end build task")
        }
    }

    fun heartbeat(executeCount: Int = 1): HeartBeatInfo {
        var retryCount = 0
        val result = HttpRetryUtils.retryWhenHttpRetryException(retryPeriodMills = retryPeriodMills) {
            if (retryCount > 0) {
                logger.warn("retryWhenHttpRetryException|time=$retryCount|heartbeat")
            }
            retryCount++
            buildApi.heartbeat(executeCount)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to do heartbeat task")
        }
        return result.data ?: throw RemoteServiceException("Failed to do heartbeat task")
    }

    fun timeout() {
        var retryCount = 0
        val result = HttpRetryUtils.retryWhenHttpRetryException(retryPeriodMills = retryPeriodMills) {
            if (retryCount > 0) {
                logger.warn("retryWhenHttpRetryException|time=$retryCount|timeout")
            }
            retryCount++
            buildApi.timeout()
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to report timeout")
        }
    }

    fun submitError(errorInfo: ErrorInfo) {
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|submitError")
                sleepInterval(retryCount)
            }
            retryCount++
            buildApi.submitError(errorInfo)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to submit error")
        }
    }
}
