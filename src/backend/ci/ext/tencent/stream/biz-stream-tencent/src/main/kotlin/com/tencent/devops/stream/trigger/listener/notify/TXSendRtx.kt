package com.tencent.devops.stream.trigger.listener.notify

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.trigger.pojo.rtxCustom.MessageType
import com.tencent.devops.stream.trigger.pojo.rtxCustom.ReceiverType
import com.tencent.devops.stream.utils.GitCIPipelineUtils

object TXSendRtx {
    fun sendRtxCustomNotify(
        accessToken: String,
        receivers: Set<String>,
        receiverType: ReceiverType,
        rtxUrl: String,
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
        v2GitUrl: String,
        content: String?,
        messageType: MessageType = MessageType.MARKDOWN,
        gitProjectId: Long
    ) {
        val realContent = if (content.isNullOrBlank()) {
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
                v2GitUrl = v2GitUrl,
                gitProjectId = gitProjectId
            )
        } else {
            getRtxCustomUserContent(
                status = status,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                build = build,
                content = content,
                v2GitUrl = v2GitUrl
            )
        }

        receivers.forEach { receiver ->
            RtxCustomApi.sendGitCIFinishMessage(
                urlPrefix = rtxUrl,
                token = accessToken,
                messageType = messageType,
                receiverType = receiverType,
                receiverId = receiver,
                content = realContent
            )
        }
    }

    // 为用户的内容增加链接
    private fun getRtxCustomUserContent(
        status: BuildStatus,
        gitProjectId: Long,
        pipelineId: String,
        build: BuildHistory,
        content: String,
        v2GitUrl: String
    ): String {
        val state = when {
            status.isSuccess() -> Triple("✔", "info", "success")
            status.isCancel() -> Triple("❕", "warning", "cancel")
            else -> Triple("❌", "warning", "failed")
        }
        val detailUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
            homePage = v2GitUrl,
            gitProjectId = gitProjectId,
            pipelineId = pipelineId,
            buildId = build.id
        )
        return " <font color=\"${state.second}\"> ${state.first} </font> $content \n [查看详情]($detailUrl)"
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
        v2GitUrl: String,
        gitProjectId: Long
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
                "Commit [[${requestId.subSequence(0, 7)}]]($gitUrl/$projectName/commit/$requestId)" +
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
            "[查看详情]" +
            "(${
            GitCIPipelineUtils.genGitCIV2BuildUrl(
                homePage = v2GitUrl,
                gitProjectId = gitProjectId,
                pipelineId = pipelineId,
                buildId = build.id
            )
            })"
    }
}
