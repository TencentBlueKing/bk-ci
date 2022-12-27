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

package com.tencent.devops.worker.common.api.engine.impl

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.ErrorInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.engine.api.pojo.HeartBeatInfo
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiPriority
import com.tencent.devops.worker.common.api.engine.EngineBuildSDKApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

@Suppress("UNUSED", "TooManyFunctions")
@ApiPriority(priority = 1)
open class EngineBuildResourceApi : AbstractBuildResourceApi(), EngineBuildSDKApi {

    protected var buildId: String? = null

    override fun getRequestUrl(path: String, retryCount: Int, executeCount: Int): String {
        return "/ms/process/$path?retryCount=$retryCount&executeCount=$executeCount&buildId=$buildId"
    }

    override fun setStarted(retryCount: Int): Result<BuildVariables> {
        val path = getRequestUrl(path = "api/build/worker/started", retryCount = retryCount)
        val request = buildPut(path)
        val errorMessage = "通知服务端启动构建失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        val result = objectMapper.readValue<Result<BuildVariables>>(responseContent)
        buildId = result.data?.buildId
        return result
    }

    override fun claimTask(retryCount: Int): Result<BuildTask> {
        val path = getRequestUrl(path = "api/build/worker/claim", retryCount = retryCount)
        val request = buildGet(path)
        val errorMessage = "领取构建机任务失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun completeTask(result: BuildTaskResult, retryCount: Int): Result<Boolean> {
        val path = getRequestUrl(path = "api/build/worker/complete", retryCount = retryCount)
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(result)
        )
        val request = buildPost(path, requestBody)
        val errorMessage = "报告任务完成失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun endTask(buildVariables: BuildVariables, retryCount: Int): Result<Boolean> {
        val path = getRequestUrl(path = "api/build/worker/end", retryCount = retryCount)
        val request = buildPost(path)
        val errorMessage = "构建完成请求失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun heartbeat(executeCount: Int): Result<HeartBeatInfo> {
        val path = getRequestUrl(path = "api/build/worker/heartbeat/v1", executeCount = executeCount)
        val request = buildPost(path)
        val errorMessage = "心跳失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 10L,
            writeTimeoutInSec = 10L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun timeout(): Result<Boolean> {
        val path = getRequestUrl(path = "api/build/worker/timeout")
        val request = buildPost(path)
        val errorMessage = "构建超时结束请求失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun submitError(errorInfo: ErrorInfo): Result<Boolean> {
        val path = getRequestUrl(path = "api/build/worker/submit_error")
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(errorInfo)
        )
        val request = buildPost(path, requestBody)
        val errorMessage = "上报启动异常信息失败"
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getJobContext(): Map<String, String> {
        return emptyMap()
    }

    override fun getBuildDetailUrl(): Result<String> {
        val path = getRequestUrl(path = "api/build/worker/detail_url")
        val request = buildGet(path)
        val errorMessage = "构建超时结束请求失败"
        val responseContent = try {
            request(
                request = request,
                connectTimeoutInSec = 5L,
                errorMessage = errorMessage,
                readTimeoutInSec = 30L,
                writeTimeoutInSec = 30L
            )
        } catch (ignore: Throwable) {
            return Result("")
        }
        return objectMapper.readValue(responseContent)
    }
}
