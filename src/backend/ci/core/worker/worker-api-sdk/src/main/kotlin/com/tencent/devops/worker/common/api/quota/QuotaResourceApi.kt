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

package com.tencent.devops.worker.common.api.quota

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode.REPORT_AGENT_END_STATUS_FAILURE
import com.tencent.devops.worker.common.constants.WorkerMessageCode.REPORT_AGENT_START_STATUS_FAILURE
import com.tencent.devops.worker.common.env.AgentEnv

class QuotaResourceApi : AbstractBuildResourceApi(), QuotaApi {

    override fun removeRunningAgent(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        retryCount: Int
    ): Result<Boolean> {
        try {
            val path = "/ms/dispatch/api/build/quotas/running/agent/shutdown?executeCount=$executeCount"
            val request = buildDelete(path)
            val errorMessage =
                MessageUtil.getMessageByLocale(REPORT_AGENT_END_STATUS_FAILURE, AgentEnv.getLocaleLanguage())
            val responseContent = request(
                request = request,
                connectTimeoutInSec = 5L,
                errorMessage = errorMessage,
                readTimeoutInSec = 30L,
                writeTimeoutInSec = 30L
            )
            return objectMapper.readValue(responseContent)
        } catch (e: Exception) {
            logger.error("Quota remove running agent failed.", e)
            return Result(false)
        }
    }

    override fun addRunningAgent(
        projectId: String,
        buildId: String,
        vmSeqId: String,
        executeCount: Int,
        retryCount: Int
    ): Result<Boolean> {
        try {
            val path = "/ms/dispatch/api/build/quotas/running/agent/start?executeCount=$executeCount"
            val request = buildPost(path)
            val errorMessage =
                MessageUtil.getMessageByLocale(REPORT_AGENT_START_STATUS_FAILURE, AgentEnv.getLocaleLanguage())
            val responseContent = request(
                request = request,
                connectTimeoutInSec = 5L,
                errorMessage = errorMessage,
                readTimeoutInSec = 30L,
                writeTimeoutInSec = 30L
            )
            return objectMapper.readValue(responseContent)
        } catch (e: Exception) {
            logger.error("Quota add running agent failed.", e)
            return Result(false)
        }
    }
}
