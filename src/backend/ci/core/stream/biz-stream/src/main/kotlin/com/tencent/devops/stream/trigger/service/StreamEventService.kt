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

package com.tencent.devops.stream.trigger.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.constant.StreamMessageCode.STARTUP_CONFIG_MISSING
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.dao.StreamUserMessageDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.message.UserMessageType
import com.tencent.devops.stream.service.StreamGitProjectInfoCache
import com.tencent.devops.stream.service.StreamWebsocketService
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.pojo.StreamMessageSaveLock
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.pojo.enums.toGitState
import com.tencent.devops.stream.util.GitCommonUtils
import com.tencent.devops.stream.util.StreamPipelineUtils
import com.tencent.devops.stream.util.StreamTriggerMessageUtils
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 用作事务存储需要附带用户消息通知的数据
 */
@Service
class StreamEventService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val streamGitConfig: StreamGitConfig,
    private val gitCheckService: GitCheckService,
    private val userMessageDao: StreamUserMessageDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val websocketService: StreamWebsocketService,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val streamGitProjectInfoCache: StreamGitProjectInfoCache
) {
    // 触发检查错误,未涉及版本解析
    fun saveTriggerNotBuildEvent(
        action: BaseAction,
        reason: String,
        reasonDetail: String?
    ): Long {
        val eventCommon = action.data.eventCommon
        return saveNotBuildEvent(
            gitProjectId = eventCommon.gitProjectId.toLong(),
            userId = eventCommon.userId,
            eventId = action.data.context.requestEventId!!,
            reason = reason,
            reasonDetail = reasonDetail,
            originYaml = null,
            parsedYaml = null,
            normalizedYaml = null,
            pipelineId = null,
            filePath = null,
            version = null,
            branch = eventCommon.branch,
            projectCode = action.getProjectCode(eventCommon.gitProjectId)
        )
    }

    // 涉及到流水线错误
    fun saveBuildNotBuildEvent(
        action: BaseAction,
        reason: String,
        reasonDetail: String,
        sendCommitCheck: Boolean,
        commitCheckBlock: Boolean,
        version: String
    ): Long {
        with(action.data) {
            if (sendCommitCheck && action.data.setting.enableCommitCheck && action.needSendCommitCheck()) {
                gitCheckService.addCommitCheck(
                    gitProjectId = eventCommon.gitProjectId,
                    commitId = eventCommon.commit.commitId,
                    gitHttpUrl = setting.gitHttpUrl,
                    scmType = streamGitConfig.getScmType(),
                    token = action.api.getToken(action.getGitCred()),
                    state = StreamCommitCheckState.FAILURE.toGitState(streamGitConfig.getScmType()),
                    block = setting.enableMrBlock && commitCheckBlock,
                    targetUrl = StreamPipelineUtils.genStreamV2NotificationsUrl(
                        streamUrl = streamGitConfig.streamUrl ?: throw ParamBlankException(
                            I18nUtil.getCodeLanMessage(
                                messageCode = STARTUP_CONFIG_MISSING,
                                params = arrayOf(" streamGitConfig")
                            )
                        ),
                        gitProjectId = getGitProjectId(),
                        messageId = action.data.context.requestEventId.toString()
                    ),
                    context = "${context.pipeline!!.filePath}@${action.metaData.streamObjectKind.name}",
                    description = TriggerReason.getTriggerReason(reason)?.summary ?: reason,
                    mrId = null,
                    addCommitCheck = action.api::addCommitCheck
                )
            }
            return saveNotBuildEvent(
                userId = eventCommon.userId,
                eventId = context.requestEventId!!,
                originYaml = context.originYaml,
                parsedYaml = context.parsedYaml,
                normalizedYaml = context.normalizedYaml,
                reason = reason,
                reasonDetail = reasonDetail,
                pipelineId = context.pipeline?.pipelineId?.ifBlank { null },
                filePath = context.pipeline?.filePath,
                gitProjectId = getGitProjectId().toLong(),
                version = version,
                branch = eventCommon.branch,
                projectCode = action.getProjectCode()
            )
        }
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
        version: String?,
        branch: String?,
        projectCode: String
    ): Long {
        val event = gitRequestEventDao.getWithEvent(dslContext = dslContext, id = eventId)
            ?: throw RuntimeException("can't find event $eventId")

        val messageId = gitRequestEventNotBuildDao.save(
            dslContext = dslContext,
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

        // eventId只用保存一次，先查询一次，如果没有在去修改
        if (saveUserMessage(
                userId = userId,
                projectCode = projectCode,
                event = event,
                gitProjectId = gitProjectId,
                messageType = UserMessageType.REQUEST,
                isSave = false // 只update
            )
        ) {
            return messageId
        }

        if (saveUserMessage(
                userId = userId,
                projectCode = projectCode,
                event = event,
                gitProjectId = gitProjectId,
                messageType = UserMessageType.REQUEST,
                isSave = true
            )
        ) {
            websocketService.pushNotifyWebsocket(
                userId,
                GitCommonUtils.getCiProjectId(gitProjectId, streamGitConfig.getScmType())
            )
        }
        return messageId
    }

    fun saveUserMessage(
        userId: String,
        projectCode: String,
        event: GitRequestEvent,
        gitProjectId: Long,
        messageType: UserMessageType,
        isSave: Boolean = true
    ): Boolean {
        val messageTitle = if (isSave) lazy {
            val checkRepoHookTrigger = gitProjectId != event.gitProjectId
            val realEvent = if (checkRepoHookTrigger) {
                // 当gitProjectId与event的不同时，说明是远程仓库触发的
                val pathWithNamespace = streamGitProjectInfoCache.getAndSaveGitProjectInfo(
                    gitProjectId = event.gitProjectId,
                    useAccessToken = true,
                    userId = userId
                )?.pathWithNamespace
                GitCommonUtils.checkAndGetRepoBranch(event, pathWithNamespace)
            } else event
            StreamTriggerMessageUtils.getEventMessageTitle(realEvent, checkRepoHookTrigger)
        } else null

        val saveLock = StreamMessageSaveLock(redisOperation, userId, projectCode, event.id.toString())
        saveLock.use {
            saveLock.lock()
            val exist = userMessageDao.getMessageExist(dslContext, projectCode, userId, event.id.toString())
            if (isSave) {
                if (exist != null) {
                    return false
                }
                userMessageDao.save(
                    dslContext = dslContext,
                    projectId = projectCode,
                    userId = userId,
                    messageType = messageType,
                    messageId = event.id.toString(),
                    messageTitle = messageTitle?.value ?: ""
                )
            } else {
                if (exist == null || exist.messageType == messageType.name) {
                    return false
                }
                userMessageDao.updateMessageType(
                    dslContext = dslContext,
                    projectId = projectCode,
                    userId = userId,
                    messageId = event.id.toString(),
                    messageType = messageType
                )
            }
        }
        return true
    }

    fun deletePipelineBuildHistory(
        pipelineIds: Set<String>
    ): Pair<Int, Int> {
        val notBuildcnt = gitRequestEventNotBuildDao.deleteNotBuildByPipelineIds(dslContext, pipelineIds)
        val buildcnt = gitRequestEventBuildDao.deleteBuildByPipelineIds(dslContext, pipelineIds)
        return Pair(buildcnt, notBuildcnt)
    }
}
