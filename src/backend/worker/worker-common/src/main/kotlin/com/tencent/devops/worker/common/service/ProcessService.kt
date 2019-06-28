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

package com.tencent.devops.worker.common.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.process.BuildSDKApi
import com.tencent.devops.worker.common.logger.LoggerService

object ProcessService {

    private val buildApi = ApiFactory.create(BuildSDKApi::class)

    fun setStarted(): BuildVariables {
        val result = buildApi.setStarted()
        if (result.isNotOk()) {
            throw RemoteServiceException("Report builder startup status failed")
        }
        return result.data ?: throw RemoteServiceException("Report builder startup status failed")
    }

    fun claimTask(): BuildTask {
        val result = buildApi.claimTask()
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to get build task")
        }
        return result.data ?: throw RemoteServiceException("Failed to get build task")
    }

    fun completeTask(
        taskId: String,
        elementId: String,
        buildResult: Map<String, String>,
        type: String?,
        message: String? = null
    ) {
        LoggerService.flush()
        val taskResult = if (message == null) {
            BuildTaskResult(taskId, elementId, true, buildResult, null, type)
        } else {
            BuildTaskResult(taskId, elementId, false, buildResult, message, type)
        }
        val result = buildApi.completeTask(taskResult)
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to complete build task")
        }
    }

    fun endBuild() {
        val result = buildApi.endTask()
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to end build task")
        }
    }

    fun heartbeat() {
        val result = buildApi.heartbeat()
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to do heartbeat task")
        }
    }
}