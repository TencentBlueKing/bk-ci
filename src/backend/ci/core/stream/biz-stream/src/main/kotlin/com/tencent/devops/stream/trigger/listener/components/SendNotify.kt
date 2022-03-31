package com.tencent.devops.stream.trigger.listener.components

import com.devops.process.yaml.v2.models.GitNotices
import com.devops.process.yaml.v2.models.IfType
import com.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.BuildHistory
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishData
import com.tencent.devops.stream.trigger.actions.data.context.getBuildStatus
import com.tencent.devops.stream.trigger.actions.data.context.getGitCommitCheckState
import com.tencent.devops.stream.trigger.actions.data.context.isSuccess
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.actions.streamActions.StreamMrAction
import com.tencent.devops.stream.trigger.listener.notify.SendEmail
import com.tencent.devops.stream.trigger.listener.notify.SendRtx
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.pojo.enums.StreamNotifyType
import com.tencent.devops.stream.trigger.pojo.rtxCustom.ReceiverType
import com.tencent.devops.stream.util.GitCommonUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SendNotify @Autowired constructor(
    private val client: Client,
    private val streamGitConfig: StreamGitConfig,
    private val streamTriggerCache: StreamTriggerCache
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SendNotify::class.java)
    }

    fun sendNotify(
        action: BaseAction
    ) {
        val build = client.get(ServiceBuildResource::class)
            .getBatchBuildStatus(
                projectId = action.getProjectCode(),
                buildId = setOf(action.data.context.finishData!!.buildId),
                channelCode = ChannelCode.GIT
            ).data.let {
                if (it.isNullOrEmpty()) {
                    throw OperationException("git ci buildInfo not exist")
                }
                it.first()
            }

        sendNotifyV2(action, build)
    }

    private fun sendNotifyV2(action: BaseAction, build: BuildHistory) {
        // 获取需要进行替换的variables
        val finishData = action.data.context.finishData!!
        val projectId = finishData.projectId
        val variables = client.get(ServiceVarResource::class)
            .getContextVar(projectId = projectId, buildId = build.id, contextName = null).data
        val notices = YamlUtil.getObjectMapper().readValue(
            finishData.normalizedYaml, ScriptBuildYaml::class.java
        ).notices
        notices?.forEach { notice ->
            // 替换 variables
            if (!checkStatus(finishData, build.id, replaceVar(notice.ifField, variables))) {
                return@forEach
            }
            val newType = getNoticeType(build.id, replaceVar(notice.type, variables)) ?: return@forEach
            sendNotifyV2(
                action = action,
                build = build,
                notice = notice,
                noticeVariables = variables,
                notifyType = newType
            )
        }
    }

    private fun sendNotifyV2(
        action: BaseAction,
        build: BuildHistory,
        notice: GitNotices,
        noticeVariables: Map<String, String>?,
        notifyType: StreamNotifyType
    ) {
        val pipeline = action.data.context.pipeline!!

        val receivers = replaceVar(notice.receivers, noticeVariables)
        val ccs = replaceVar(notice.ccs, noticeVariables)?.toMutableSet()
        val chatIds = replaceVar(notice.chatId, noticeVariables)?.toMutableSet()
        val title = replaceVar(notice.title, noticeVariables)
        val content = replaceVar(notice.content, noticeVariables)
        val projectName = GitCommonUtils.getRepoName(action.data.setting.gitHttpUrl, action.data.setting.name)

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
            realReceivers = mutableSetOf(build.userId)
        }
        val state = action.data.context.finishData!!.getGitCommitCheckState()

        when (notifyType) {
            StreamNotifyType.EMAIL -> {
                val request = SendEmail.getEmailSendRequest(
                    state = state,
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
                    gitProjectId = action.data.eventCommon.gitProjectId
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            StreamNotifyType.RTX_CUSTOM, StreamNotifyType.RTX_GROUP -> {
                // 目前开源版通过蓝盾notify仅支持了single通知，企业微信群通知关键字请自行实现
                if (notifyType == StreamNotifyType.RTX_GROUP) {
                    return
                }

                val (rtxReceivers, receiversType) = Pair(realReceivers, ReceiverType.SINGLE)

                SendRtx.getRtxSendRequest(
                    state = state,
                    receivers = rtxReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipeline.pipelineId,
                    build = build,
                    isMr = action.metaData.isStreamMr(),
                    requestId = requestId,
                    openUser = build.userId,
                    buildTime = build.totalTime,
                    gitUrl = streamGitConfig.getGitUrl(action.data.setting.scmType),
                    streamUrl = streamGitConfig.streamUrl!!,
                    content = content,
                    gitProjectId = action.data.eventCommon.gitProjectId,
                    scmType = action.data.setting.scmType
                )
            }
            else -> {
                return
            }
        }
    }

    // 校验V2通知状态
    private fun checkStatus(
        finishData: BuildFinishData,
        buildId: String,
        ifField: String?
    ): Boolean {
        // 未填写则所有状态都发送
        if (ifField.isNullOrBlank()) {
            return true
        }
        // stage审核的状态专门判断为成功
        val success = finishData.isSuccess()
        return when (ifField) {
            IfType.SUCCESS.name -> {
                return success
            }
            IfType.FAILURE.name -> {
                return !success
            }
            IfType.CANCELLED.name -> {
                return finishData.getBuildStatus().isCancel()
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

    private fun getNoticeType(buildId: String, type: String?): StreamNotifyType? {
        return when (type) {
            "email" -> {
                StreamNotifyType.EMAIL
            }
            "wework-message" -> {
                StreamNotifyType.RTX_CUSTOM
            }
            "wework-chat" -> {
                StreamNotifyType.RTX_GROUP
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
