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

package com.tencent.devops.gitci.listener

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.v2.IfType
import com.tencent.devops.common.ci.v2.ScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.pojo.enums.GitCICommitCheckState
import com.tencent.devops.gitci.pojo.enums.GitCINotifyTemplateEnum
import com.tencent.devops.gitci.pojo.enums.GitCINotifyType
import com.tencent.devops.gitci.pojo.rtxCustom.MessageType
import com.tencent.devops.gitci.pojo.rtxCustom.ReceiverType
import com.tencent.devops.gitci.pojo.v2.GitCIBasicSetting
import com.tencent.devops.gitci.utils.GitCIPipelineUtils
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.gitci.v2.dao.GitCIBasicSettingDao
import com.tencent.devops.model.gitci.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServiceVarResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.Date

@Service
class GitCIBuildFinishListener @Autowired constructor(
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCISettingDao: GitCISettingDao,
    private val gitCIBasicSettingDao: GitCIBasicSettingDao,
    private val client: Client,
    private val scmClient: ScmClient,
    private val dslContext: DSLContext
) {

    @Value("\${rtx.corpid:#{null}}")
    private val corpId: String? = null

    @Value("\${rtx.corpsecret:#{null}}")
    private val corpSecret: String? = null

    @Value("\${rtx.url:#{null}}")
    private val rtxUrl: String? = null

    @Value("\${rtx.gitUrl:#{null}}")
    private val gitUrl: String? = null

    @Value("\${rtx.v2GitUrl:#{null}}")
    private val v2GitUrl: String? = null

    private val buildSuccessDesc = "Your pipeline「%s」 is succeed."
    private val buildCancelDesc = "Your pipeline「%s」 was cancelled."
    private val buildFailedDesc = "Your pipeline「%s」 is failed."

    @RabbitListener(
        bindings = [(QueueBinding(
            value = Queue(value = MQ.QUEUE_PIPELINE_BUILD_FINISH_GITCI, durable = "true"),
            exchange = Exchange(
                value = MQ.EXCHANGE_PIPELINE_BUILD_FINISH_FANOUT,
                durable = "true",
                delayed = "true",
                type = ExchangeTypes.FANOUT
            )
        ))]
    )

    @Suppress("ALL")
    fun listenPipelineBuildFinishBroadCastEvent(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        try {
            val record = gitRequestEventBuildDao.getEventByBuildId(dslContext, buildFinishEvent.buildId)
            if (record != null) {
                val pipelineId = record["PIPELINE_ID"] as String
                logger.info("listenPipelineBuildFinishBroadCastEvent , " +
                    "pipelineId : $pipelineId, buildFinishEvent: $buildFinishEvent")

                val objectKind = record["OBJECT_KIND"] as String
                val buildStatus = BuildStatus.valueOf(buildFinishEvent.status)
                // 检测状态
                val state = if (buildStatus.isSuccess()) {
                    GitCICommitCheckState.SUCCESS
                } else {
                    GitCICommitCheckState.FAILURE
                }

                val commitId = record["COMMIT_ID"] as String
                val gitProjectId = record["GIT_PROJECT_ID"] as Long
                var mergeRequestId = 0L
                if (record["MERGE_REQUEST_ID"] != null) {
                    mergeRequestId = record["MERGE_REQUEST_ID"] as Long
                }

                val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId)
                val v2GitSetting = gitCIBasicSettingDao.getSetting(dslContext, gitProjectId)
                if (gitProjectConf == null && v2GitSetting == null) {
                    throw OperationException("git ci all projectCode not exist")
                }

                val description = if (record["DESCRIPTION"] != null) {
                    record["DESCRIPTION"] as String
                } else ""

                val pipeline = gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId)
                    ?: throw OperationException("git ci pipeline not exist")

                val event = gitRequestEventBuildDao.getByBuildId(dslContext, buildFinishEvent.buildId)
                    ?: throw OperationException("git ci buildEvent not exist")

                // 检查yml版本，根据yml版本选择不同的实现
                val isV2 = (!event.version.isNullOrBlank() && event.version == "v2.0")

                if (isV2) {
                    if (v2GitSetting == null) {
                        throw OperationException("git ci v2 projectCode not exist")
                    }
                } else {
                    if (gitProjectConf == null) {
                        throw OperationException("git ci projectCode not exist")
                    }
                }

                // 推送结束构建消息,当人工触发时不推送CommitCheck消息
                if (objectKind != OBJECT_KIND_MANUAL) {
                    if (isV2) {
                        scmClient.pushCommitCheck(
                            commitId = commitId,
                            description = getDescByBuildStatus(description, buildStatus, pipeline.displayName),
                            mergeRequestId = mergeRequestId,
                            buildId = buildFinishEvent.buildId,
                            userId = buildFinishEvent.userId,
                            status = state,
                            context = pipeline.filePath,
                            gitCIBasicSetting = v2GitSetting!!,
                            pipelineId = buildFinishEvent.pipelineId,
                            block = (objectKind == OBJECT_KIND_MERGE_REQUEST && !buildStatus.isSuccess())
                        )
                    } else {
                        scmClient.pushCommitCheck(
                            commitId = commitId,
                            description = description,
                            mergeRequestId = mergeRequestId,
                            buildId = buildFinishEvent.buildId,
                            userId = buildFinishEvent.userId,
                            status = state,
                            context = "${pipeline.displayName}(${pipeline.filePath})",
                            gitProjectConf = gitProjectConf!!
                        )
                    }
                }

                // 发送通知兼容v1的老数据
                // v1 校验是否发送通知
                if (!isV2 && !checkIsSendNotify(gitProjectConf!!, state.value)) {
                    return
                }

                val sourceProjectId = if (record["SOURCE_GIT_PROJECT_ID"] == null) {
                    null
                } else {
                    record["SOURCE_GIT_PROJECT_ID"] as Long
                }

                val buildInfo = client.get(ServiceBuildResource::class)
                    .getBatchBuildStatus(
                        projectId = if (isV2) {
                            v2GitSetting!!.projectCode!!
                        } else {
                            gitProjectConf!!.projectCode!!
                        },
                        buildId = setOf(buildFinishEvent.buildId),
                        channelCode = ChannelCode.GIT
                    ).data
                if (buildInfo == null || buildInfo.isEmpty()) {
                    throw OperationException("git ci buildInfo not exist")
                }

                // 构建结束发送通知
                val build = buildInfo.first()
                if (isV2) {
                    // 获取需要进行替换的variables
                    val variables =
                        client.get(ServiceVarResource::class).getBuildVar(buildId = build.id, varName = null).data
                    val notices = YamlUtil.getObjectMapper().readValue(
                        event.normalizedYaml, ScriptBuildYaml::class.java
                    ).notices
                    notices?.forEach { notice ->
                        // 替换 variables
                        if (!checkStatus(build.id, replaceVar(notice.ifField, variables), buildStatus)) {
                            return@forEach
                        }
                        val newType = replaceVar(notice.type, variables)
                        if (newType.isNullOrBlank()) {
                            return@forEach
                        }
                        sendNotifyV2(
                            gitProjectId = gitProjectId,
                            sourceProjectId = sourceProjectId,
                            mergeRequestId = mergeRequestId,
                            commitId = commitId,
                            state = state.value,
                            conf = v2GitSetting!!,
                            event = event,
                            pipeline = pipeline,
                            build = build,
                            receivers = replaceVar(notice.receivers, variables),
                            ccs = replaceVar(notice.ccs, variables)?.toMutableSet(),
                            chatIds = replaceVar(notice.chatId, variables)?.toMutableSet(),
                            title = replaceVar(notice.title, variables),
                            content = replaceVar(notice.content, variables),
                            notifyType = getNoticeType(build.id, newType)
                        )
                    }
                } else {
                    notify(
                        gitProjectId = gitProjectId,
                        sourceProjectId = sourceProjectId,
                        mergeRequestId = mergeRequestId,
                        commitId = commitId,
                        state = state.value,
                        conf = gitProjectConf!!,
                        event = event,
                        pipeline = pipeline,
                        build = build
                    )
                }
                // 更新流水线执行状态
                gitRequestEventBuildDao.updateBuildStatusById(
                    dslContext = dslContext,
                    id = record["ID"] as Long,
                    buildStatus = buildStatus
                )
            }
        } catch (e: Throwable) {
            logger.error("Fail to push commit check build(${buildFinishEvent.buildId})", e)
        }
    }

    // 根据状态切换描述
    private fun getDescByBuildStatus(oldDesc: String?, buildStatus: BuildStatus, pipelineName: String): String {
        return when {
            !oldDesc.isNullOrBlank() -> {
                oldDesc
            }
            buildStatus.isSuccess() -> {
                buildSuccessDesc.format(pipelineName)
            }
            buildStatus.isCancel() -> {
                buildCancelDesc.format(pipelineName)
            }
            else -> {
                buildFailedDesc.format(pipelineName)
            }
        }
    }

    // 校验V2通知状态
    private fun checkStatus(buildId: String, ifField: String?, buildStatus: BuildStatus): Boolean {
        // 未填写则所有状态都发送
        if (ifField.isNullOrBlank()) {
            return true
        }
        return when (ifField) {
            IfType.SUCCESS.name -> {
                return buildStatus.isSuccess()
            }
            IfType.FAILURE.name -> {
                return buildStatus.isFailure()
            }
            IfType.CANCELLED.name -> {
                return buildStatus.isCancel()
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

    // 校验是否发送通知
    private fun checkIsSendNotify(conf: GitRepositoryConf, state: String): Boolean {
        // 老数据不发送通知
        if (conf.rtxCustomProperty == null || conf.rtxGroupProperty == null || conf.emailProperty == null) {
            logger.warn("gitCI project: ${conf.gitProjectId} enable notify but not have config")
            return false
        }
        if (conf.onlyFailedNotify == null) {
            return false
        }
        // 仅在失败时发送通知
        if (conf.onlyFailedNotify!! && state != "failure") {
            return false
        }
        return true
    }

    private fun notify(
        gitProjectId: Long,
        sourceProjectId: Long?,
        mergeRequestId: Long,
        commitId: String,
        state: String,
        conf: GitRepositoryConf,
        event: TGitRequestEventBuildRecord,
        pipeline: TGitPipelineResourceRecord,
        build: BuildHistory
    ) {
        if (conf.rtxCustomProperty?.enabled == true) {
            sendNotify(
                gitProjectId = gitProjectId,
                sourceProjectId = sourceProjectId,
                mergeRequestId = mergeRequestId,
                commitId = commitId,
                state = state,
                notifyType = GitCINotifyType.RTX_CUSTOM,
                conf = conf,
                event = event,
                pipeline = pipeline,
                build = build
            )
        }

        if (conf.rtxGroupProperty?.enabled == true) {
            sendNotify(
                gitProjectId = gitProjectId,
                sourceProjectId = sourceProjectId,
                mergeRequestId = mergeRequestId,
                commitId = commitId,
                state = state,
                notifyType = GitCINotifyType.RTX_GROUP,
                conf = conf,
                event = event,
                pipeline = pipeline,
                build = build
            )
        }

        if (conf.emailProperty?.enabled == true) {
            sendNotify(
                gitProjectId = gitProjectId,
                sourceProjectId = sourceProjectId,
                mergeRequestId = mergeRequestId,
                commitId = commitId,
                state = state,
                notifyType = GitCINotifyType.EMAIL,
                conf = conf,
                event = event,
                pipeline = pipeline,
                build = build
            )
        }
    }

    private fun sendNotify(
        gitProjectId: Long,
        sourceProjectId: Long?,
        mergeRequestId: Long,
        commitId: String,
        state: String,
        notifyType: GitCINotifyType,
        conf: GitRepositoryConf,
        event: TGitRequestEventBuildRecord,
        pipeline: TGitPipelineResourceRecord,
        build: BuildHistory
    ) {

        val projectName = getProjectName(conf.gitHttpUrl, conf.name)
        val branchName = GitCommonUtils.checkAndGetForkBranchName(
            gitProjectId = gitProjectId,
            sourceGitProjectId = sourceProjectId,
            branch = event.branch,
            client = client
        )
        val pipelineName = pipeline.displayName ?: pipeline.filePath.replace(".yml", "")
        val isMr = mergeRequestId != 0L
        val requestId = if (isMr) {
            mergeRequestId.toString()
        } else {
            commitId
        }
        val buildNum = build.buildNum.toString()

        when (notifyType) {
            GitCINotifyType.EMAIL -> {
                var realReceivers = replaceReceivers(conf.emailProperty!!.receivers, build.buildParameters)
                if (realReceivers.isEmpty()) {
                    realReceivers = mutableSetOf(build.userId)
                }
                val request = getEmailSendRequest(
                    state = state,
                    receivers = realReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    build = build,
                    commitId = commitId,
                    pipelineId = pipeline.pipelineId
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            GitCINotifyType.RTX_CUSTOM -> {
                var realReceivers = replaceReceivers(conf.rtxCustomProperty!!.receivers, build.buildParameters)
                // 接收人默认带触发人
                if (realReceivers.isEmpty()) {
                    realReceivers = mutableSetOf(build.userId)
                }
                val accessToken =
                    RtxCustomApi.getAccessToken(urlPrefix = rtxUrl, corpSecret = corpSecret, corpId = corpId)
                val content = getRtxCustomContent(
                    isSuccess = state == "success",
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipeline.pipelineId,
                    buildNum = buildNum,
                    isMr = isMr,
                    requestId = requestId,
                    buildTime = build.totalTime,
                    openUser = build.userId
                )
                sendRtxCustomNotify(
                    accessToken = accessToken,
                    content = content,
                    messageType = MessageType.MARKDOWN,
                    receiverType = ReceiverType.SINGLE,
                    receivers = realReceivers
                )
            }
            GitCINotifyType.RTX_GROUP -> {
                val realGroups = replaceReceivers(conf.rtxGroupProperty!!.groupIds, build.buildParameters)
                if (conf.rtxCustomProperty == null || conf.rtxCustomProperty!!.receivers.isEmpty()) {
                    logger.warn("notifyRtxGroups receivers is null ")
                    return
                }
                val accessToken =
                    RtxCustomApi.getAccessToken(urlPrefix = rtxUrl, corpSecret = corpSecret, corpId = corpId)
                val content = getRtxCustomContent(
                    isSuccess = state == "success",
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    pipelineId = pipeline.pipelineId,
                    buildNum = buildNum,
                    isMr = isMr,
                    requestId = requestId,
                    buildTime = build.totalTime,
                    openUser = build.userId
                )
                sendRtxCustomNotify(
                    accessToken = accessToken,
                    content = content,
                    messageType = MessageType.MARKDOWN,
                    receiverType = ReceiverType.GROUP,
                    receivers = realGroups
                )
            }
            else -> return
        }
    }

    private fun getEmailSendRequest(
        state: String,
        receivers: Set<String>,
        projectName: String,
        branchName: String,
        pipelineName: String,
        build: BuildHistory,
        commitId: String,
        pipelineId: String
    ): SendNotifyMessageTemplateRequest {
        val notifyTemplateEnum = if (state == "success") {
            GitCINotifyTemplateEnum.GITCI_BUILD_SUCCESS_TEMPLATE
        } else {
            GitCINotifyTemplateEnum.GITCI_BUILD_FAILED_TEMPLATE
        }
        val titleParams = mapOf(
            "projectName" to projectName,
            "branchName" to branchName,
            "pipelineName" to pipelineName,
            "buildNum" to build.buildNum.toString()
        )
        val bodyParams = mapOf(
            "projectName" to projectName,
            "branchName" to branchName,
            "pipelineName" to pipelineName,
            "buildNum" to build.buildNum.toString(),
            "startTime" to DateTimeUtil.formatDate(Date(build.startTime), "yyyy-MM-dd HH:mm"),
            "totalTime" to DateTimeUtil.formatMillSecond(build.totalTime ?: 0),
            "trigger" to build.userId,
            "commitId" to commitId,
            "webUrl" to "$gitUrl/$projectName/ci/pipelines#/detail/$pipelineId/?pipelineName=$pipelineName"
        )
        return SendNotifyMessageTemplateRequest(
            templateCode = notifyTemplateEnum.templateCode,
            receivers = receivers.toMutableSet(),
            cc = mutableSetOf(),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.EMAIL.name)
        )
    }

    private fun getRtxCustomContent(
        isSuccess: Boolean,
        projectName: String,
        branchName: String,
        pipelineName: String,
        pipelineId: String,
        buildNum: String,
        isMr: Boolean,
        requestId: String,
        openUser: String,
        buildTime: Long?
    ): String {
        val state = if (isSuccess) {
            Triple("✔", "info", "success")
        } else {
            Triple("❌", "warning", "failed")
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
            "$projectName($branchName) - $pipelineName #$buildNum run ${state.third} \n " +
            request +
            costTime +
            "[View it on  工蜂内网版]" +
            "($gitUrl/$projectName/ci/pipelines#/detail/$pipelineId/?pipelineName=$pipelineName)"
    }

    private fun sendRtxCustomNotify(
        accessToken: String,
        content: String,
        receivers: Set<String>,
        messageType: MessageType,
        receiverType: ReceiverType
    ) {
        receivers.forEach { receiver ->
            RtxCustomApi.sendGitCIFinishMessage(
                urlPrefix = rtxUrl,
                token = accessToken,
                messageType = messageType,
                receiverType = receiverType,
                receiverId = receiver,
                content = content
            )
        }
    }

    // 获取 name/projectName格式的项目名称
    private fun getProjectName(gitHttpUrl: String, name: String): String {
        return try {
            GitCommonUtils.getRepoName(gitHttpUrl, name)
        } catch (e: Exception) {
            name
        }
    }

    private fun sendNotifyV2(
        gitProjectId: Long,
        sourceProjectId: Long?,
        mergeRequestId: Long,
        commitId: String,
        state: String,
        notifyType: GitCINotifyType?,
        conf: GitCIBasicSetting,
        receivers: Set<String>?,
        ccs: MutableSet<String>?,
        chatIds: Set<String>?,
        event: TGitRequestEventBuildRecord,
        pipeline: TGitPipelineResourceRecord,
        build: BuildHistory,
        title: String?,
        content: String?
    ) {
        val projectName = getProjectName(conf.gitHttpUrl, conf.name)
        val branchName = GitCommonUtils.checkAndGetForkBranchName(
            gitProjectId = gitProjectId,
            sourceGitProjectId = sourceProjectId,
            branch = event.branch,
            client = client
        )
        val pipelineName = pipeline.displayName ?: pipeline.filePath.replace(".yml", "")
        val isMr = mergeRequestId != 0L
        val requestId = if (isMr) {
            mergeRequestId.toString()
        } else {
            commitId
        }
        var realReceivers = replaceReceivers(receivers, build.buildParameters)
        // 接收人默认带触发人
        if (realReceivers.isEmpty()) {
            realReceivers = mutableSetOf(build.userId)
        }
        when (notifyType) {
            GitCINotifyType.EMAIL -> {
                val request = getEmailSendRequestV2(
                    state = state,
                    receivers = realReceivers,
                    projectName = projectName,
                    branchName = branchName,
                    pipelineName = pipelineName,
                    build = build,
                    commitId = commitId,
                    pipelineId = pipeline.pipelineId,
                    title = title,
                    content = content,
                    ccs = ccs
                )
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            GitCINotifyType.RTX_CUSTOM, GitCINotifyType.RTX_GROUP -> {
                val accessToken =
                    RtxCustomApi.getAccessToken(urlPrefix = rtxUrl, corpSecret = corpSecret, corpId = corpId)
                val newContent = if (content.isNullOrBlank()) {
                    getRtxCustomContentV2(
                        isSuccess = state == "success",
                        projectName = projectName,
                        branchName = branchName,
                        pipelineName = pipelineName,
                        pipelineId = pipeline.pipelineId,
                        build = build,
                        isMr = isMr,
                        requestId = requestId,
                        buildTime = build.totalTime,
                        openUser = build.userId
                    )
                } else {
                    getRtxCustomContentV2(
                        isSuccess = state == "success",
                        projectName = projectName,
                        pipelineId = pipeline.pipelineId,
                        build = build,
                        content = content
                    )
                }
                if (notifyType == GitCINotifyType.RTX_GROUP) {
                    val realGroups = replaceReceivers(chatIds, build.buildParameters)
                    sendRtxCustomNotify(
                        accessToken = accessToken,
                        content = newContent,
                        messageType = MessageType.MARKDOWN,
                        receiverType = ReceiverType.GROUP,
                        receivers = realGroups
                    )
                } else {
                    sendRtxCustomNotify(
                        accessToken = accessToken,
                        content = newContent,
                        messageType = MessageType.MARKDOWN,
                        receiverType = ReceiverType.SINGLE,
                        receivers = realReceivers
                    )
                }
            }
            else -> {
                return
            }
        }
    }

    private fun getEmailSendRequestV2(
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
        content: String?
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
                        homePage = v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                        projectName = projectName,
                        pipelineId = pipelineId,
                        buildId = build.id
                    )
                )
            } else {
                content
            })
        )
        return SendNotifyMessageTemplateRequest(
            templateCode = GitCINotifyTemplateEnum.GITCI_V2_BUILD_TEMPLATE.templateCode,
            receivers = receivers.toMutableSet(),
            cc = ccs,
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.EMAIL.name)
        )
    }

    // 为用户的内容增加链接
    private fun getRtxCustomContentV2(
        isSuccess: Boolean,
        projectName: String,
        pipelineId: String,
        build: BuildHistory,
        content: String
    ): String {
        val state = if (isSuccess) {
            Triple("✔", "info", "success")
        } else {
            Triple("❌", "warning", "failed")
        }
        val detailUrl = GitCIPipelineUtils.genGitCIV2BuildUrl(
            homePage = v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
            projectName = projectName,
            pipelineId = pipelineId,
            buildId = build.id
        )
        return " <font color=\"${state.second}\"> ${state.first} </font> $content \n [查看详情]($detailUrl)"
    }

    private fun getRtxCustomContentV2(
        isSuccess: Boolean,
        projectName: String,
        branchName: String,
        pipelineName: String,
        pipelineId: String,
        build: BuildHistory,
        isMr: Boolean,
        requestId: String,
        openUser: String,
        buildTime: Long?
    ): String {
        val state = if (isSuccess) {
            Triple("✔", "info", "success")
        } else {
            Triple("❌", "warning", "failed")
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
                    homePage = v2GitUrl ?: throw ParamBlankException("启动配置缺少 rtx.v2GitUrl"),
                    projectName = projectName,
                    pipelineId = pipelineId,
                    buildId = build.id
                )
            })"
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

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBuildFinishListener::class.java)
    }
}
