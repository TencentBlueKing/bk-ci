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

package com.tencent.devops.worker.common.api.process

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BUILD_FINISH_REQUEST_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.BUILD_TIMEOUT_END_REQUEST_FAILURE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_BUILD_TASK_DETAILS_FAILURE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.HEARTBEAT_FAIL
import com.tencent.devops.worker.common.constants.WorkerMessageCode.NOTIFY_SERVER_START_BUILD_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.RECEIVE_BUILD_MACHINE_TASK_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.REPORT_TASK_FINISH_FAILURE
import com.tencent.devops.worker.common.env.AgentEnv
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class BuildResourceApi : AbstractBuildResourceApi(), BuildSDKApi {

    override fun setStarted(): Result<BuildVariables> {
        val path = "/ms/process/api/build/builds/started"
        val request = buildPut(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            NOTIFY_SERVER_START_BUILD_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun claimTask(): Result<BuildTask> {
        val path = "/ms/process/api/build/builds/claim"
        val request = buildGet(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            RECEIVE_BUILD_MACHINE_TASK_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun completeTask(result: BuildTaskResult): Result<Boolean> {
        val path = "/ms/process/api/build/builds/complete"
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(result)
        )
        val request = buildPost(path, requestBody)
        val errorMessage = MessageUtil.getMessageByLocale(
            REPORT_TASK_FINISH_FAILURE, AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun endTask(): Result<Boolean> {
        val path = "/ms/process/api/build/builds/end"
        val request = buildPost(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            BUILD_FINISH_REQUEST_FAILED,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun heartbeat(): Result<Boolean> {
        val path = "/ms/process/api/build/builds/heartbeat"
        val request = buildPost(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            HEARTBEAT_FAIL,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 10L,
            writeTimeoutInSec = 10L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getSingleHistoryBuild(
        projectId: String,
        pipelineId: String,
        buildNum: String,
        channelCode: ChannelCode?
    ): Result<BuildHistory?> {
        val sb = StringBuilder("/ms/process/api/build/builds/$projectId/$pipelineId/$buildNum/history")
        if (channelCode != null) sb.append("?channelCode=${channelCode.name}")
        val path = sb.toString()
        val request = buildGet(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            GET_BUILD_TASK_DETAILS_FAILURE,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getBuildDetail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        channelCode: ChannelCode
    ): Result<ModelDetail?> {
        val sb = StringBuilder(
            "/ms/process/api/build/builds/$projectId/$pipelineId/$buildId/" +
                "detail?channelCode=${channelCode.name}"
        )
        val path = sb.toString()
        val request = buildGet(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            GET_BUILD_TASK_DETAILS_FAILURE,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun timeout(): Result<Boolean> {

        val path = "/ms/process/api/build/builds/timeout"
        val request = buildPost(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            BUILD_TIMEOUT_END_REQUEST_FAILURE,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun getBuildAcrossTemplateInfo(
        templateId: String
    ): Result<List<BuildTemplateAcrossInfo>> {
        val sb = StringBuilder(
            "/ms/process/api/build/templates/across?templateId=$templateId"
        )
        val path = sb.toString()
        val request = buildGet(path)
        val errorMessage = MessageUtil.getMessageByLocale(
            GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE,
            AgentEnv.getLocaleLanguage()
        )
        val responseContent = request(
            request = request,
            connectTimeoutInSec = 5L,
            errorMessage = errorMessage,
            readTimeoutInSec = 30L,
            writeTimeoutInSec = 30L
        )
        return objectMapper.readValue(responseContent)
    }
}
