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

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_VIEW_DETAILS
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.trigger.pojo.enums.StreamNotifyTemplateEnum
import com.tencent.devops.stream.util.StreamPipelineUtils

object SendRtx {
    fun getRtxSendRequest(
        status: BuildStatus,
        receivers: Set<String>,
        projectName: String,
        branchName: String,
        pipelineName: String,
        pipelineId: String,
        build: BuildHistory,
        isMr: Boolean,
        requestId: String,
        openUser: String,
        buildTime: Long?,
        gitUrl: String,
        streamUrl: String,
        content: String?,
        gitProjectId: String,
        scmType: ScmType
    ): SendNotifyMessageTemplateRequest {
        val titleParams = mapOf(
            "title" to ""
        )
        val bodyParams = mapOf(
            "content" to if (content.isNullOrBlank()) {
                getRtxCustomContent(
                    status = status,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipelineId,
                    build = build,
                    isMr = isMr,
                    requestId = requestId,
                    openUser = openUser,
                    buildTime = buildTime,
                    gitUrl = gitUrl,
                    streamUrl = streamUrl,
                    gitProjectId = gitProjectId
                )
            } else {
                getRtxCustomUserContent(
                    status = status,
                    gitProjectId = gitProjectId,
                    pipelineId = pipelineId,
                    build = build,
                    content = content,
                    streamUrl = streamUrl
                )
            }
        )
        return SendNotifyMessageTemplateRequest(
            templateCode = StreamNotifyTemplateEnum.STREAM_V2_BUILD_TEMPLATE.templateCode,
            receivers = receivers.toMutableSet(),
            cc = null,
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name)
        )
    }

    // 为用户的内容增加链接
    private fun getRtxCustomUserContent(
        status: BuildStatus,
        gitProjectId: String,
        pipelineId: String,
        build: BuildHistory,
        content: String,
        streamUrl: String
    ): String {
        val state = when {
            status.isSuccess() -> Triple("✔", "info", "success")
            status.isCancel() -> Triple("❕", "warning", "cancel")
            else -> Triple("❌", "warning", "failed")
        }

        val detailUrl = StreamPipelineUtils.genStreamV2BuildUrl(
            homePage = streamUrl,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            buildId = build.id
        )
        return " <font color=\"${state.second}\"> ${state.first} </font>" +
                " $content \n [${I18nUtil.getCodeLanMessage(BK_VIEW_DETAILS)}]($detailUrl)"
    }

    private fun getRtxCustomContent(
        status: BuildStatus,
        projectName: String,
        branchName: String,
        pipelineName: String,
        pipelineId: String,
        build: BuildHistory,
        isMr: Boolean,
        requestId: String,
        openUser: String,
        buildTime: Long?,
        gitUrl: String,
        streamUrl: String,
        gitProjectId: String
    ): String {
        val state = when {
            status.isSuccess() -> Triple("✔", "info", "success")
            status.isCancel() -> Triple("❕", "warning", "cancel")
            else -> Triple("❌", "warning", "failed")
        }

        val request = if (isMr) {
            "Merge requests [[!$requestId]]($gitUrl/$projectName/merge_requests/$requestId)" +
                    "opened by $openUser \n"
        } else {
            if (requestId.length >= 8) {
                "Commit [[${requestId.subSequence(0, 8)}]]($gitUrl/$projectName/commit/$requestId)" +
                        "pushed by $openUser \n"
            } else {
                "Manual Triggered by $openUser \n"
            }
        }
        val costTime = "Time cost ${DateTimeUtil.formatMillSecond(buildTime ?: 0)}.  \n   "
        return " <font color=\"${state.second}\"> ${state.first} </font> " +
                "$projectName($branchName) - $pipelineName #${build.buildNum} run ${state.third} \n " +
                request +
                costTime +
                "[${I18nUtil.getCodeLanMessage(BK_VIEW_DETAILS)}]" +
                "(${
                    StreamPipelineUtils.genStreamV2BuildUrl(
                        homePage = streamUrl,
                        gitProjectId = gitProjectId,
                        pipelineId = pipelineId,
                        buildId = build.id
                    )
                })"
    }
}
