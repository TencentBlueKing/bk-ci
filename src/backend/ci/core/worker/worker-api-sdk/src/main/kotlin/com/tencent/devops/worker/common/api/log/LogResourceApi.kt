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

package com.tencent.devops.worker.common.api.log

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.log.pojo.TaskBuildLogProperty
import com.tencent.devops.common.log.pojo.enums.LogStorageMode
import com.tencent.devops.common.log.pojo.message.LogMessage
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.LOGS_END_STATUS_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.LOGS_REPORT_FAILED
import com.tencent.devops.worker.common.constants.WorkerMessageCode.LOG_STORAGE_STATUS_FAILED
import com.tencent.devops.worker.common.env.AgentEnv
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class LogResourceApi : AbstractBuildResourceApi(), LogSDKApi {

    override fun addLogMultiLine(buildId: String, logMessages: List<LogMessage>): Result<Boolean> {
        val path = "/log/api/build/logs/multi?buildId=$buildId"
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(logMessages)
        )
        val request = buildPost(path, requestBody)
        val responseContent = request(
            request = request,
            errorMessage = MessageUtil.getMessageByLocale(LOGS_REPORT_FAILED, AgentEnv.getLocaleLanguage()),
            connectTimeoutInSec = 5L,
            readTimeoutInSec = 10L,
            writeTimeoutInSec = 10L
        )
        return objectMapper.readValue(responseContent)
    }

    override fun finishLog(
        tag: String?,
        jobId: String?,
        executeCount: Int?,
        subTag: String?,
        logMode: LogStorageMode?
    ): Result<Boolean> {
        val path = StringBuilder("/log/api/build/logs/status?finished=true")
        if (!tag.isNullOrBlank()) path.append("&tag=$tag")
        if (!subTag.isNullOrBlank()) path.append("&subTag=$subTag")
        if (!jobId.isNullOrBlank()) path.append("&jobId=$jobId")
        if (executeCount != null) path.append("&executeCount=$executeCount")
        if (logMode != null) path.append("&logMode=${logMode.name}")
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), "")
        val request = buildPut(path.toString(), requestBody)
        val responseContent = request(
            request,
            MessageUtil.getMessageByLocale(LOGS_END_STATUS_FAILED, AgentEnv.getLocaleLanguage())
        )
        return objectMapper.readValue(responseContent)
    }

    override fun updateStorageMode(
        propertyList: List<TaskBuildLogProperty>,
        executeCount: Int
    ): Result<Boolean> {
        val path = StringBuilder("/log/api/build/logs/mode")
        path.append("?executeCount=$executeCount")
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            objectMapper.writeValueAsString(propertyList)
        )
        val request = buildPost(path.toString(), requestBody)
        val responseContent = request(
            request = request,
            errorMessage = MessageUtil.getMessageByLocale(
                LOG_STORAGE_STATUS_FAILED,
                AgentEnv.getLocaleLanguage()
            ),
            connectTimeoutInSec = 5L,
            readTimeoutInSec = 10L,
            writeTimeoutInSec = 10L
        )
        return objectMapper.readValue(responseContent)
    }
}
