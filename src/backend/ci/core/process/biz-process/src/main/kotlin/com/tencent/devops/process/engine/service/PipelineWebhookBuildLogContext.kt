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

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLog
import com.tencent.devops.process.pojo.webhook.PipelineWebhookBuildLogDetail
import java.time.LocalDateTime

object PipelineWebhookBuildLogContext {
    private val currentTriggerLog = ThreadLocal<PipelineWebhookBuildLog>()

    fun initTriggerLog(
        codeType: String,
        requestContent: String
    ) {
        currentTriggerLog.set(
            PipelineWebhookBuildLog(
                codeType = codeType,
                requestContent = requestContent,
                receivedTime = LocalDateTime.now().timestampmilli(),
                createdTime = LocalDateTime.now().timestampmilli()
            )
        )
    }

    fun addRepoInfo(
        repoName: String,
        commitId: String
    ) {
        val triggerLog = with(currentTriggerLog.get()) {
            PipelineWebhookBuildLog(
                id = id,
                codeType = codeType,
                requestContent = requestContent,
                receivedTime = receivedTime,
                createdTime = createdTime,
                repoName = repoName,
                commitId = commitId
            )
        }
        currentTriggerLog.set(triggerLog)
    }

    @Suppress("ALL")
    fun addLogBuildInfo(
        projectId: String,
        pipelineId: String,
        taskId: String,
        taskName: String,
        success: Boolean,
        triggerResult: String?,
        id: Long? = null
    ) {
        val webhookLog = currentTriggerLog.get()
        webhookLog.detail.add(
            PipelineWebhookBuildLogDetail(
                codeType = webhookLog.codeType,
                repoName = webhookLog.repoName!!,
                commitId = webhookLog.commitId!!,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = taskId,
                taskName = taskName,
                success = success,
                triggerResult = triggerResult,
                createdTime = LocalDateTime.now().timestampmilli(),
                id = id
            )
        )
    }

    fun get(): PipelineWebhookBuildLog {
        return currentTriggerLog.get()
    }

    fun remove() {
        currentTriggerLog.remove()
    }
}
