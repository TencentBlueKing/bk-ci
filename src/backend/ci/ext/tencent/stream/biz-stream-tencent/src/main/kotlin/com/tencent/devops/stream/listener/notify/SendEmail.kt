package com.tencent.devops.stream.listener.notify

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.pojo.enums.GitCINotifyTemplateEnum
import com.tencent.devops.stream.utils.GitCIPipelineUtils
import java.util.Date

object SendEmail {
    fun getEmailSendRequest(
        state: String,
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
        v2GitUrl: String,
        gitProjectId: Long
    ): SendNotifyMessageTemplateRequest {
        val isSuccess = state == "success"
        val titleParams = mapOf(
            "title" to (if (title.isNullOrBlank()) {
                V2NotifyTemplate.getEmailTitle(
                    isSuccess = isSuccess,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName, buildNum = build.buildNum.toString()
                )
            } else {
                title
            })
        )
        val bodyParams = mapOf(
            "content" to (if (content.isNullOrBlank()) {
                V2NotifyTemplate.getEmailContent(
                    isSuccess = isSuccess,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    buildNum = build.buildNum.toString(),
                    startTime = DateTimeUtil.formatDate(Date(build.startTime), "yyyy-MM-dd HH:mm"),
                    totalTime = DateTimeUtil.formatMillSecond(build.totalTime ?: 0),
                    trigger = build.userId,
                    commitId = commitId,
                    webUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
                        homePage = v2GitUrl,
                        gitProjectId = gitProjectId,
                        pipelineId = pipelineId,
                        buildId = build.id
                    )
                )
            } else {
                content
            })
        )
        return SendNotifyMessageTemplateRequest(
            templateCode = GitCINotifyTemplateEnum.STREAM_V2_BUILD_TEMPLATE.templateCode,
            receivers = receivers.toMutableSet(),
            cc = ccs,
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.EMAIL.name)
        )
    }
}
