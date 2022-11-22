package com.tencent.devops.stream.trigger.listener.components

import com.tencent.devops.common.client.Client
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.stream.config.StreamBuildFinishConfig
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.context.getBuildStatus
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.listener.notify.RtxCustomApi
import com.tencent.devops.stream.trigger.listener.notify.SendEmail
import com.tencent.devops.stream.trigger.listener.notify.TXSendRtx
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.pojo.enums.StreamNotifyType
import com.tencent.devops.stream.trigger.pojo.rtxCustom.ReceiverType
import com.tencent.devops.stream.util.GitCommonUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
class TXSendNotify @Autowired constructor(
    private val client: Client,
    private val config: StreamBuildFinishConfig,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache
) : SendNotify(
    client, streamGitConfig, streamTriggerCache
) {

    override fun sendNotifyV2(
        action: BaseAction,
        build: BuildHistory,
        notice: GitNotices,
        noticeVariables: Map<String, String>?,
        notifyType: StreamNotifyType
    ) {
        val pipeline = action.data.context.pipeline!!

        val receivers = replaceSetVar(notice.receivers, noticeVariables)
        val ccs = replaceSetVar(notice.ccs, noticeVariables)?.toMutableSet()
        val chatIds = replaceSetVar(notice.chatId, noticeVariables)?.toMutableSet()
        val title = replaceVar(notice.title, noticeVariables)
        val content = replaceVar(notice.content, noticeVariables)
        val projectName = action.data.eventCommon.gitProjectName ?: GitCommonUtils.getRepoName(
            action.data.setting.gitHttpUrl,
            action.data.setting.name
        )

        val gitProjectInfoCache = action.data.eventCommon.sourceGitProjectId?.let {
            streamTriggerCache.getAndSaveRequestGitProjectInfo(
                gitProjectKey = it,
                action = action,
                getProjectInfo = action.api::getGitProjectInfo
            )
        }
        val branchName = GitCommonUtils.checkAndGetForkBranchName(
            gitProjectId = action.data.eventCommon.gitProjectId.toLong(),
            sourceGitProjectId = action.data.eventCommon.sourceGitProjectId?.toLong(),
            branch = action.data.eventCommon.branch,
            pathWithNamespace = gitProjectInfoCache?.pathWithNamespace
        )
        val pipelineName = pipeline.displayName
        val requestId = if (action is StreamMrAction) {
            action.mrIId
        } else {
            action.data.eventCommon.commit.commitId
        }
        var realReceivers = replaceReceivers(receivers, build.buildParameters)
        // 接收人默认带触发人
        if (realReceivers.isEmpty()) {
            realReceivers = mutableSetOf(action.data.eventCommon.userId)
        }
        val status = action.data.context.finishData!!.getBuildStatus()

        when (notifyType) {
            StreamNotifyType.EMAIL -> {
                val request = SendEmail.getEmailSendRequest(
                    status = status,
                    receivers = realReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    build = build,
                    commitId = action.data.eventCommon.commit.commitId,
                    pipelineId = pipeline.pipelineId,
                    title = title,
                    content = content,
                    ccs = ccs,
                    streamUrl = streamGitConfig.streamUrl!!,
                    gitProjectId = action.data.getGitProjectId()
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            StreamNotifyType.RTX_CUSTOM, StreamNotifyType.RTX_GROUP -> {
                val accessToken = RtxCustomApi.getAccessToken(
                    urlPrefix = config.rtxUrl,
                    corpId = config.corpId,
                    corpSecret = config.corpSecret
                )
                val (rtxReceivers, receiversType) = if (notifyType == StreamNotifyType.RTX_GROUP) {
                    Pair(replaceReceivers(chatIds, build.buildParameters), ReceiverType.GROUP)
                } else {
                    Pair(realReceivers, ReceiverType.SINGLE)
                }
                TXSendRtx.sendRtxCustomNotify(
                    accessToken = accessToken,
                    receivers = rtxReceivers,
                    receiverType = receiversType,
                    rtxUrl = config.rtxUrl!!,
                    status = status,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipeline.pipelineId,
                    build = build,
                    isMr = action.metaData.isStreamMr(),
                    requestId = requestId,
                    openUser = build.userId,
                    buildTime = build.totalTime,
                    gitUrl = config.gitUrl!!,
                    v2GitUrl = config.v2GitUrl!!,
                    content = content,
                    gitProjectId = action.data.getGitProjectId().toLong()
                )
            }
            else -> {
                return
            }
        }
    }
}
