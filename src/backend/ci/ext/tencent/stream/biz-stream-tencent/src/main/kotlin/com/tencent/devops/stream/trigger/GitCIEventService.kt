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

package com.tencent.devops.stream.trigger

import com.tencent.devops.stream.client.ScmClient
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.stream.pojo.v2.message.UserMessageType
import com.tencent.devops.stream.utils.StreamTriggerMessageUtils
import com.tencent.devops.stream.v2.dao.StreamUserMessageDao
import com.tencent.devops.stream.v2.service.TXStreamBasicSettingService
import com.tencent.devops.stream.v2.service.StreamWebsocketService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 用作事务存储需要附带用户消息通知的数据
 */
@Service
class GitCIEventService @Autowired constructor(
    private val dslContext: DSLContext,
    private val scmClient: ScmClient,
    private val userMessageDao: StreamUserMessageDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val TXStreamBasicSettingService: TXStreamBasicSettingService,
    private val websocketService: StreamWebsocketService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val eventMessageUtil: StreamTriggerMessageUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(GitCIEventService::class.java)
    }

    // 触发检查错误,未涉及版本解析
    fun saveTriggerNotBuildEvent(
        gitProjectId: Long,
        userId: String,
        eventId: Long,
        reason: String,
        reasonDetail: String?,
        branch: String?
    ): Long {
        return saveNotBuildEvent(
            gitProjectId = gitProjectId,
            userId = userId,
            eventId = eventId,
            reason = reason,
            reasonDetail = reasonDetail,
            originYaml = null,
            parsedYaml = null,
            normalizedYaml = null,
            pipelineId = null,
            filePath = null,
            version = null,
            branch = branch
        )
    }

    // 解析yaml错误
    fun saveBuildNotBuildEvent(
        userId: String,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String,
        reasonDetail: String,
        pipelineId: String?,
        pipelineName: String?,
        filePath: String,
        gitProjectId: Long,
        sendCommitCheck: Boolean,
        commitCheckBlock: Boolean,
        version: String?,
        branch: String?
    ): Long {
        val event = gitRequestEventDao.getWithEvent(dslContext = dslContext, id = eventId)
            ?: throw RuntimeException("can't find event $eventId")
        val gitBasicSetting = TXStreamBasicSettingService.getGitCIConf(gitProjectId)
            ?: throw RuntimeException("can't find gitBasicSetting $gitProjectId")
        // 人工触发不发送
        if (gitBasicSetting.enableCommitCheck &&
            event.objectKind != TGitObjectKind.MANUAL.value && sendCommitCheck
        ) {
            val realBlock = gitBasicSetting.enableMrBlock && commitCheckBlock
            scmClient.pushCommitCheckWithBlock(
                commitId = event.commitId,
                mergeRequestId = event.mergeRequestId ?: 0L,
                userId = event.userId,
                block = realBlock,
                state = GitCICommitCheckState.FAILURE,
                context = "$filePath@${event.objectKind.toUpperCase()}",
                description = TriggerReason.getTriggerReason(reason)?.summary ?: reason,
                gitCIBasicSetting = gitBasicSetting,
                jumpNotification = true
            )
        }
        return saveNotBuildEvent(
            userId = userId,
            eventId = eventId,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = normalizedYaml,
            reason = reason,
            reasonDetail = reasonDetail,
            pipelineId = pipelineId,
            filePath = filePath,
            gitProjectId = gitProjectId,
            gitEvent = event,
            version = version,
            branch = branch
        )
    }

    // 流水线启动错误
    fun saveRunNotBuildEvent(
        userId: String,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String,
        reasonDetail: String,
        pipelineId: String?,
        pipelineName: String?,
        filePath: String,
        gitProjectId: Long,
        sendCommitCheck: Boolean,
        commitCheckBlock: Boolean,
        version: String?,
        branch: String?
    ): Long {
        return saveBuildNotBuildEvent(
            userId = userId,
            eventId = eventId,
            originYaml = originYaml,
            parsedYaml = parsedYaml,
            normalizedYaml = normalizedYaml,
            reason = reason,
            reasonDetail = reasonDetail,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            filePath = filePath,
            gitProjectId = gitProjectId,
            sendCommitCheck = sendCommitCheck,
            commitCheckBlock = commitCheckBlock,
            version = version,
            branch = branch
        )
    }

    private fun saveNotBuildEvent(
        userId: String,
        eventId: Long,
        originYaml: String?,
        parsedYaml: String? = null,
        normalizedYaml: String?,
        reason: String?,
        reasonDetail: String?,
        pipelineId: String?,
        filePath: String?,
        gitProjectId: Long,
        gitEvent: GitRequestEvent? = null,
        version: String?,
        branch: String?
    ): Long {
        var messageId = -1L
        val event = gitEvent ?: (gitRequestEventDao.getWithEvent(dslContext = dslContext, id = eventId)
            ?: throw RuntimeException("can't find event $eventId"))
        val messageTitle = eventMessageUtil.getEventMessageTitle(event)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            messageId = gitRequestEventNotBuildDao.save(
                dslContext = context,
                eventId = eventId,
                originYaml = originYaml,
                parsedYaml = parsedYaml,
                normalizedYaml = normalizedYaml,
                reason = reason,
                reasonDetail = reasonDetail,
                pipelineId = pipelineId,
                filePath = filePath,
                gitProjectId = gitProjectId,
                version = version,
                branch = branch
            )
            // eventId只用保存一次
            if (!userMessageDao.getMessageExist(context, "git_$gitProjectId", userId, event.id.toString())) {
                userMessageDao.save(
                    dslContext = context,
                    projectId = "git_$gitProjectId",
                    userId = userId,
                    messageType = UserMessageType.REQUEST,
                    messageId = event.id.toString(),
                    messageTitle = messageTitle
                )
                websocketService.pushNotifyWebsocket(userId, gitProjectId.toString())
            }
        }
        return messageId
    }

    fun deletePipelineBuildHistory(
        pipelineIds: Set<String>
    ): Pair<Int, Int> {
        val notBuildcnt = gitRequestEventNotBuildDao.deleteNotBuildByPipelineIds(dslContext, pipelineIds)
        val buildcnt = gitRequestEventBuildDao.deleteBuildByPipelineIds(dslContext, pipelineIds)
        return Pair(buildcnt, notBuildcnt)
    }
}
