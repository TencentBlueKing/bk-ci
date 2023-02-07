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

package com.tencent.devops.stream.trigger.listener.notify

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.trigger.pojo.enums.StreamNotifyTemplateEnum
import com.tencent.devops.stream.util.StreamPipelineUtils
import java.util.Date

object SendEmail {
    fun getEmailSendRequest(
        status: BuildStatus,
        receivers: Set<String>,
        ccs: MutableSet<String>?,
        projectName: String,
        branchName: String,
        pipelineName: String,
        build: BuildHistory,
        commitId: String,
        pipelineId: String,
        title: String?,
        content: String?,
        streamUrl: String,
        gitProjectId: String
    ): SendNotifyMessageTemplateRequest {
        val titleParams = mapOf(
            "title" to (
                if (title.isNullOrBlank()) {
                    V2NotifyTemplate.getEmailTitle(
                        status = status,
                        projectName = projectName,
                        branchName = branchName,
                        pipelineName = pipelineName, buildNum = build.buildNum.toString()
                    )
                } else {
                    title
                }
                )
        )
        val buildNum = build.buildNum.toString()
        val startTime = DateTimeUtil.formatDate(Date(build.startTime), "yyyy-MM-dd HH:mm")
        val totalTime = DateTimeUtil.formatMillSecond(build.totalTime ?: 0)
        val trigger = build.userId
        val webUrl = StreamPipelineUtils.genStreamV2BuildUrl(
            homePage = streamUrl,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            buildId = build.id
        )
        val bodyParams = mapOf(
            "content" to (
                if (content.isNullOrBlank()) {
                    V2NotifyTemplate.getEmailContent(
                        status = status,
                        projectName = projectName,
                        branchName = branchName,
                        pipelineName = pipelineName,
                        buildNum = buildNum,
                        startTime = startTime,
                        totalTime = totalTime,
                        trigger = trigger,
                        commitId = commitId,
                        webUrl = webUrl
                    )
                } else {
                    content
                }
                ),
            "buildNum" to buildNum,
            "startTime" to startTime,
            "totalTime" to totalTime,
            "trigger" to trigger,
            "branchName" to branchName,
            "commitId" to commitId,
            "webUrl" to webUrl
        )
        return SendNotifyMessageTemplateRequest(
            templateCode = StreamNotifyTemplateEnum.STREAM_V2_BUILD_TEMPLATE.templateCode,
            receivers = receivers.toMutableSet(),
            cc = ccs,
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.EMAIL.name)
        )
    }
}
