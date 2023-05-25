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

package com.tencent.devops.stream.trigger.listener.components

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
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.process.yaml.v2.models.IfType
import com.tencent.devops.process.yaml.v2.models.ScriptBuildYaml
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.context.BuildFinishData
import com.tencent.devops.stream.trigger.actions.data.context.getBuildStatus
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
        val variables = client.get(ServiceVarResource::class).getContextVar(
            projectId = finishData.projectId,
            pipelineId = finishData.pipelineId,
            buildId = build.id,
            contextName = null
        ).data
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

    protected fun sendNotifyV2(
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
                // 目前开源版通过蓝盾notify仅支持了single通知，企业微信群通知关键字请自行实现
                if (notifyType == StreamNotifyType.RTX_GROUP) {
                    return
                }

                val (rtxReceivers, receiversType) = Pair(realReceivers, ReceiverType.SINGLE)

                SendRtx.getRtxSendRequest(
                    status = status,
                    receivers = rtxReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipeline.pipelineId,
                    build = build,
                    isMr = action.metaData.isStreamMr(),
                    requestId = requestId,
                    openUser = action.data.eventCommon.userId,
                    buildTime = build.totalTime,
                    gitUrl = streamGitConfig.gitUrl!!,
                    streamUrl = streamGitConfig.streamUrl!!,
                    content = content,
                    gitProjectId = action.data.getGitProjectId(),
                    scmType = streamGitConfig.getScmType()
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

        return when (ifField) {
            IfType.SUCCESS.name -> {
                // stage审核的状态专门判断为成功
                return finishData.isSuccess()
            }
            IfType.FAILURE.name -> {
                return finishData.getBuildStatus().isFailure()
            }
            IfType.CANCELLED.name, IfType.CANCELED.name -> {
                return finishData.getBuildStatus().isCancel()
            }
            IfType.ALWAYS.name -> {
                return true
            }
            else -> {
                logger.warn("SendNotify|checkStatus|buildId|$buildId|ifField|$ifField")
                false
            }
        }
    }

    // 替换variables变量
    protected fun replaceVar(value: String?, variables: Map<String, String>?): String? {
        if (value.isNullOrBlank()) {
            return value
        }
        if (variables.isNullOrEmpty()) {
            return value
        }
        return EnvUtils.parseEnv(value, variables)
    }

    // #7592 支持通过 , 分隔来一次填写多个接收人
    protected fun replaceSetVar(value: Set<String>?, variables: Map<String, String>?): Set<String>? {
        if (value.isNullOrEmpty()) {
            return value
        }

        val vars = mutableSetOf<String>()
        value.forEach { re ->
            vars.addAll(re.parseEnv(variables)
                .split(",").asSequence().filter { it.isNotBlank() }.map { it.trim() }.toSet()
            )
        }

        if (variables.isNullOrEmpty()) {
            return vars
        }
        return vars.map {
            EnvUtils.parseEnv(it, variables)
        }.toSet()
    }

    private fun String.parseEnv(data: Map<String, String>?) =
        if (!data.isNullOrEmpty()) EnvUtils.parseEnv(this, data) else this

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
                logger.warn("SendNotify|getNoticeType|buidld|$buildId|type|$type")
                null
            }
        }
    }

    // 使用启动参数替换接收人
    protected fun replaceReceivers(receivers: Set<String>?, startParams: List<BuildParameters>?): MutableSet<String> {
        if (receivers.isNullOrEmpty()) {
            return mutableSetOf()
        }

        if (startParams.isNullOrEmpty()) {
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
