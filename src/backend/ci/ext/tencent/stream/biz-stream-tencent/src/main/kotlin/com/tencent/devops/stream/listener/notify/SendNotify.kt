package com.tencent.devops.stream.listener.notify

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.GitNotices
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.config.StreamBuildFinishConfig
import com.tencent.devops.stream.listener.StreamBuildListenerContext
import com.tencent.devops.stream.listener.StreamBuildListenerContextV2
import com.tencent.devops.stream.listener.StreamFinishContextV1
import com.tencent.devops.stream.listener.getBuildStatus
import com.tencent.devops.stream.listener.getGitCommitCheckState
import com.tencent.devops.stream.listener.isSuccess
import com.tencent.devops.stream.pojo.enums.GitCINotifyType
import com.tencent.devops.stream.pojo.isMr
import com.tencent.devops.stream.pojo.rtxCustom.ReceiverType
import com.tencent.devops.stream.trigger.StreamGitProjectInfoCache
import com.tencent.devops.stream.utils.GitCommonUtils
import com.tencent.devops.stream.v2.service.StreamScmService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SendNotify @Autowired constructor(
    private val client: Client,
    private val config: StreamBuildFinishConfig,
    private val streamGitProjectInfoCache: StreamGitProjectInfoCache,
    private val streamScmService: StreamScmService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SendNotify::class.java)
    }

    fun sendNotify(
        context: StreamBuildListenerContext
    ) {
        // v1 校验是否发送通知，v1发通知功能未有使用，直接下掉
        if (context is StreamFinishContextV1) {
            return
        }

        val build = client.get(ServiceBuildResource::class)
            .getBatchBuildStatus(
                projectId = GitCommonUtils.getCiProjectId(context.requestEvent.gitProjectId),
                buildId = setOf(context.buildEvent.buildId),
                channelCode = ChannelCode.GIT
            ).data.let {
                if (it.isNullOrEmpty()) {
                    throw OperationException("git ci buildInfo not exist")
                }
                it.first()
            }

        sendNotifyV2(context as StreamBuildListenerContextV2, build)
    }

    private fun sendNotifyV2(context: StreamBuildListenerContextV2, build: BuildHistory) {
        with(context) {
            // 获取需要进行替换的variables
            val projectId = context.buildEvent.projectId
            val variables = client.get(ServiceVarResource::class)
                .getContextVar(projectId = projectId, buildId = build.id, contextName = null).data
            val notices = YamlUtil.getObjectMapper().readValue(
                streamBuildEvent.normalizedYaml, ScriptBuildYaml::class.java
            ).notices
            notices?.forEach { notice ->
                // 替换 variables
                if (!checkStatus(this, build.id, replaceVar(notice.ifField, variables))) {
                    return@forEach
                }
                val newType = getNoticeType(build.id, replaceVar(notice.type, variables)) ?: return@forEach
                sendNotifyV2(
                    context = context,
                    build = build,
                    notice = notice,
                    noticeVariables = variables,
                    notifyType = newType
                )
            }
        }
    }

    private fun sendNotifyV2(
        context: StreamBuildListenerContextV2,
        build: BuildHistory,
        notice: GitNotices,
        noticeVariables: Map<String, String>?,
        notifyType: GitCINotifyType
    ) {
        val receivers = replaceVar(notice.receivers, noticeVariables)
        val ccs = replaceVar(notice.ccs, noticeVariables)?.toMutableSet()
        val chatIds = replaceVar(notice.chatId, noticeVariables)?.toMutableSet()
        val title = replaceVar(notice.title, noticeVariables)
        val content = replaceVar(notice.content, noticeVariables)

        val projectName = GitCommonUtils.getRepoName(context.streamSetting.gitHttpUrl, context.streamSetting.name)
        val gitProjectInfoCache = context.requestEvent.sourceGitProjectId?.let {
            lazy {
                streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                    gitProjectId = it,
                    useAccessToken = true,
                    getProjectInfo = streamScmService::getProjectInfoRetry
                )
            }
        }
        val branchName = GitCommonUtils.checkAndGetForkBranchName(
            gitProjectId = context.requestEvent.gitProjectId,
            sourceGitProjectId = context.requestEvent.sourceGitProjectId,
            branch = context.requestEvent.branch,
            gitProjectCache = gitProjectInfoCache
        )
        val pipelineName = context.pipeline.displayName
        val requestId = if (context.requestEvent.isMr()) {
            context.requestEvent.mergeRequestId.toString()
        } else {
            context.requestEvent.commitId
        }
        var realReceivers = replaceReceivers(receivers, build.buildParameters)
        // 接收人默认带触发人
        if (realReceivers.isEmpty()) {
            realReceivers = mutableSetOf(build.userId)
        }
        val state = context.getGitCommitCheckState().value

        when (notifyType) {
            GitCINotifyType.EMAIL -> {
                val request = SendEmail.getEmailSendRequest(
                    state = state,
                    receivers = realReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    build = build,
                    commitId = context.requestEvent.commitId,
                    pipelineId = context.pipeline.pipelineId,
                    title = title,
                    content = content,
                    ccs = ccs,
                    v2GitUrl = config.v2GitUrl!!,
                    gitProjectId = context.requestEvent.gitProjectId
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            GitCINotifyType.RTX_CUSTOM, GitCINotifyType.RTX_GROUP -> {
                val accessToken = RtxCustomApi.getAccessToken(
                    urlPrefix = config.rtxUrl,
                    corpId = config.corpId,
                    corpSecret = config.corpSecret
                )
                val (rtxReceivers, receiversType) = if (notifyType == GitCINotifyType.RTX_GROUP) {
                    Pair(replaceReceivers(chatIds, build.buildParameters), ReceiverType.GROUP)
                } else {
                    Pair(realReceivers, ReceiverType.SINGLE)
                }
                SendRtx.sendRtxCustomNotify(
                    accessToken = accessToken,
                    receivers = rtxReceivers,
                    receiverType = receiversType,
                    rtxUrl = config.rtxUrl!!,
                    isSuccess = state == "success",
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = context.pipeline.pipelineId,
                    build = build,
                    isMr = context.requestEvent.isMr(),
                    requestId = requestId,
                    openUser = build.userId,
                    buildTime = build.totalTime,
                    gitUrl = config.gitUrl!!,
                    v2GitUrl = config.v2GitUrl!!,
                    content = content,
                    gitProjectId = context.requestEvent.gitProjectId
                )
            }
            else -> {
                return
            }
        }
    }

    // 校验V2通知状态
    private fun checkStatus(
        context: StreamBuildListenerContextV2,
        buildId: String,
        ifField: String?
    ): Boolean {
        // 未填写则所有状态都发送
        if (ifField.isNullOrBlank()) {
            return true
        }
        // stage审核的状态专门判断为成功
        val success = context.isSuccess()
        return when (ifField) {
            IfType.SUCCESS.name -> {
                return success
            }
            IfType.FAILURE.name -> {
                return !success
            }
            IfType.CANCELLED.name, IfType.CANCELED.name -> {
                return context.getBuildStatus().isCancel()
            }
            IfType.ALWAYS.name -> {
                return true
            }
            else -> {
                logger.error("buidld: $buildId , ifField: $ifField is error!")
                false
            }
        }
    }

    // 替换variables变量
    private fun replaceVar(value: String?, variables: Map<String, String>?): String? {
        if (value.isNullOrBlank()) {
            return value
        }
        if (variables.isNullOrEmpty()) {
            return value
        }
        return EnvUtils.parseEnv(value, variables)
    }

    private fun replaceVar(value: Set<String>?, variables: Map<String, String>?): Set<String>? {
        if (value.isNullOrEmpty()) {
            return value
        }
        if (variables.isNullOrEmpty()) {
            return value
        }
        return value.map {
            EnvUtils.parseEnv(it, variables)
        }.toSet()
    }

    private fun getNoticeType(buildId: String, type: String?): GitCINotifyType? {
        return when (type) {
            "email" -> {
                GitCINotifyType.EMAIL
            }
            "wework-message" -> {
                GitCINotifyType.RTX_CUSTOM
            }
            "wework-chat" -> {
                GitCINotifyType.RTX_GROUP
            }
            else -> {
                logger.error("buidld: $buildId , type: $type is error!")
                null
            }
        }
    }

    // 使用启动参数替换接收人
    private fun replaceReceivers(receivers: Set<String>?, startParams: List<BuildParameters>?): MutableSet<String> {
        if (receivers == null || receivers.isEmpty()) {
            return mutableSetOf()
        }
        if (startParams == null || startParams.isEmpty()) {
            return receivers.toMutableSet()
        }
        val paramMap = startParams.associate {
            it.key to it.value.toString()
        }
        return receivers.map { receiver ->
            EnvUtils.parseEnv(receiver, paramMap)
        }.toMutableSet()
    }
}
