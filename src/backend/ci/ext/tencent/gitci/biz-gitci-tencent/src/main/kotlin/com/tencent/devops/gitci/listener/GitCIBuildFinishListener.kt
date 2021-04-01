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
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.PipelineBuildFinishBroadCastEvent
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitPipelineResourceDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.gitci.pojo.enums.GitCINotifyTemplateEnum
import com.tencent.devops.gitci.pojo.enums.GitCINotifyType
import com.tencent.devops.gitci.pojo.rtxCustom.MessageType
import com.tencent.devops.gitci.pojo.rtxCustom.ReceiverType
import com.tencent.devops.gitci.utils.GitCommonUtils
import com.tencent.devops.model.gitci.tables.records.TGitPipelineResourceRecord
import com.tencent.devops.model.gitci.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.api.service.ServiceBuildResource
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

@Service
class GitCIBuildFinishListener @Autowired constructor(
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitPipelineResourceDao: GitPipelineResourceDao,
    private val gitCISettingDao: GitCISettingDao,
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
    fun listenPipelineBuildFinishBroadCastEvent(buildFinishEvent: PipelineBuildFinishBroadCastEvent) {
        try {
            val record = gitRequestEventBuildDao.getEventByBuildId(dslContext, buildFinishEvent.buildId)
            if (record != null) {
                val pipelineId = record["PIPELINE_ID"] as String
                logger.info("listenPipelineBuildFinishBroadCastEvent , pipelineId : $pipelineId, buildFinishEvent: $buildFinishEvent")

                val objectKind = record["OBJECT_KIND"] as String

                // 检测状态
                val state = if (BuildStatus.isFailure(BuildStatus.valueOf(buildFinishEvent.status))) {
                    "failure"
                } else {
                    "success"
                }

                val commitId = record["COMMIT_ID"] as String
                val gitProjectId = record["GIT_PROJECT_ID"] as Long
                var mergeRequestId = 0L
                if (record["MERGE_REQUEST_ID"] != null) {
                    mergeRequestId = record["MERGE_REQUEST_ID"] as Long
                }

                val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId)
                    ?: throw OperationException("git ci projectCode not exist")

                // 构建结束后取消mr锁定
                if (objectKind == OBJECT_KIND_MERGE_REQUEST) {
                    scmClient.pushCommitCheckWithBlock(
                        commitId = commitId,
                        mergeRequestId = mergeRequestId,
                        userId = buildFinishEvent.userId,
                        context = "${buildFinishEvent.pipelineId}(${buildFinishEvent.buildId})",
                        block = false,
                        gitProjectConf = gitProjectConf
                    )
                }

                val description = record["DESCRIPTION"] as String

                val pipeline = gitPipelineResourceDao.getPipelineById(dslContext, gitProjectId, pipelineId)
                    ?: throw OperationException("git ci pipeline not exist")

                // 推送结束构建消息,当人工触发时不推送CommitCheck消息
                if (objectKind != OBJECT_KIND_MANUAL) {
                    scmClient.pushCommitCheck(
                        commitId = commitId,
                        description = description,
                        mergeRequestId = mergeRequestId,
                        buildId = buildFinishEvent.buildId,
                        userId = buildFinishEvent.userId,
                        status = state,
                        context = "${pipeline!!.displayName}(${pipeline.filePath})",
                        gitProjectConf = gitProjectConf
                    )
                }

                if (!checkIsSendNotify(conf = gitProjectConf, state = state)) {
                    return
                }

                val sourceProjectId = if (record["SOURCE_GIT_PROJECT_ID"] == null) {
                    null
                } else {
                    record["SOURCE_GIT_PROJECT_ID"] as Long
                }
                val event = gitRequestEventBuildDao.getByBuildId(dslContext, buildFinishEvent.buildId)
                    ?: throw OperationException("git ci buildEvent not exist")
                val buildInfo = client.get(ServiceBuildResource::class)
                    .getBatchBuildStatus(
                        projectId = gitProjectConf.projectCode!!,
                        buildId = setOf(buildFinishEvent.buildId),
                        channelCode = ChannelCode.GIT
                    ).data
                if (buildInfo == null || buildInfo.isEmpty()) {
                    throw OperationException("git ci buildInfo not exist")
                }

                // 构建结束发送通知
                notify(
                    gitProjectId = gitProjectId,
                    sourceProjectId = sourceProjectId,
                    mergeRequestId = mergeRequestId,
                    commitId = commitId,
                    state = state,
                    conf = gitProjectConf,
                    event = event,
                    pipeline = pipeline,
                    build = buildInfo.first()
                )
            }
        } catch (e: Throwable) {
            logger.error("Fail to push commit check build(${buildFinishEvent.buildId})", e)
        }
    }

    // 校验是否发送通知
    private fun checkIsSendNotify(conf: GitRepositoryConf, state: String): Boolean {
        // 老数据不发送通知
        if (conf.enableNotify == null || !conf.enableNotify!!) {
            return false
        }
        if (conf.notifyType == null || conf.notifyType!!.isEmpty()) {
            logger.warn("gitCI project: ${conf.gitProjectId} enable notify but not have notifyType")
            return false
        }
        if (conf.isFailedNotify == null) {
            return false
        }
        // 仅在失败时发送通知
        if (conf.isFailedNotify!! && state != "failure") {
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
        conf.notifyType!!.forEach {
            sendNotify(
                gitProjectId = gitProjectId,
                sourceProjectId = sourceProjectId,
                mergeRequestId = mergeRequestId,
                commitId = commitId,
                state = state,
                notify = it,
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
        notify: GitCINotifyType,
        conf: GitRepositoryConf,
        event: TGitRequestEventBuildRecord,
        pipeline: TGitPipelineResourceRecord,
        build: BuildHistory
    ) {
        var receivers = replaceReceivers(conf.notifyReceivers, build.buildParameters)

        val projectName = getProjectName(conf)
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

        when (notify) {
            GitCINotifyType.EMAIL -> {
                if (receivers.isEmpty()) {
                    receivers = mutableSetOf(build.userId)
                }
                val request = getEmailSendRequest(state, receivers, projectName, branchName, pipelineName, buildNum)
                client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
            }
            GitCINotifyType.RTX_CUSTOM, GitCINotifyType.RTX_GROUP -> {
                if (notify == GitCINotifyType.RTX_GROUP &&
                    (conf.notifyRtxGroups == null || conf.notifyRtxGroups!!.isEmpty())
                ) {
                    logger.warn("notifyRtxGroups receivers is null ")
                    return
                }
                // 接收人默认带触发人
                if (notify == GitCINotifyType.RTX_CUSTOM && receivers.isEmpty()) {
                    receivers = mutableSetOf(build.userId)
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
                    receiverType = if (notify == GitCINotifyType.RTX_CUSTOM) {
                        ReceiverType.SINGLE
                    } else {
                        ReceiverType.GROUP
                    },
                    receivers = if (notify == GitCINotifyType.RTX_CUSTOM) {
                        receivers
                    } else {
                        conf.notifyRtxGroups!!
                    }
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
        buildNum: String
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
            "buildNum" to buildNum
        )
        val bodyParams = mapOf(
            "content" to "邮件通知测试"
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
            "Commit [[${requestId.subSequence(0, 7)}]]($gitUrl/$projectName/commit/$requestId)" +
                    "pushed by $openUser \n"
        }
        val costTime = if (buildTime == null) {
            ""
        } else if (buildTime < 60) {
            "Time cost ${buildTime}s.  \n   "
        } else {
            "Time cost ${buildTime / 60}m ${buildTime % 60}s.  \n   "
        }
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
    private fun getProjectName(conf: GitRepositoryConf): String {
        return try {
            val names = conf.homepage.split("/")
            val userName = names[names.lastIndex - 1]
            val projectName = names.last()
            "$userName/$projectName"
        } catch (e: Exception) {
            conf.name
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
        val paramMap = startParams.map {
            it.key to it.value.toString()
        }.toMap()
        return receivers.map { receiver ->
            EnvUtils.parseEnv(receiver, paramMap)
        }.toMutableSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBuildFinishListener::class.java)
    }
}
