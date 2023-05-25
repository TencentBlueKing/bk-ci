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

package com.tencent.devops.stream.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPullRequestEvent
import com.tencent.devops.common.webhook.pojo.code.github.GithubPushEvent
import com.tencent.devops.model.stream.tables.records.TGitRequestEventBuildRecord
import com.tencent.devops.model.stream.tables.records.TGitRequestEventNotBuildRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.dao.GitRequestEventBuildDao
import com.tencent.devops.stream.dao.GitRequestEventDao
import com.tencent.devops.stream.dao.GitRequestEventNotBuildDao
import com.tencent.devops.stream.dao.StreamUserMessageDao
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.message.ContentAttr
import com.tencent.devops.stream.pojo.message.RequestMessageContent
import com.tencent.devops.stream.pojo.message.UserMessage
import com.tencent.devops.stream.pojo.message.UserMessageRecord
import com.tencent.devops.stream.pojo.message.UserMessageType
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.util.GitCommonUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class StreamUserMessageService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val streamUserMessageDao: StreamUserMessageDao,
    private val websocketService: StreamWebsocketService,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao,
    private val pipelineResourceDao: GitPipelineResourceDao,
    private val streamGitConfig: StreamGitConfig,
    private val actionFactory: EventActionFactory
) {
    companion object {
        private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val logger = LoggerFactory.getLogger(StreamUserMessageService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getMessages(
        projectId: String?,
        userId: String,
        messageType: UserMessageType?,
        haveRead: Boolean?,
        messageId: String?,
        triggerUserId: String?,
        page: Int,
        pageSize: Int
    ): Page<UserMessageRecord> {
        val startEpoch = System.currentTimeMillis()
        val gitProjectId = if (projectId == null) {
            null
        } else {
            GitCommonUtils.getGitProjectId(projectId)
        }
        // 后续有不同类型再考虑分开逻辑，目前全部按照request处理
        // 后台单独做项目级别的信息获取兼容
        val messageCount = if (projectId != null) {
            streamUserMessageDao.getMessageCount(
                dslContext = dslContext,
                projectId = projectId,
                userId = triggerUserId,
                messageType = messageType,
                messageId = messageId,
                haveRead = haveRead
            )
        } else {
            streamUserMessageDao.getMessageCount(
                dslContext = dslContext,
                projectId = "",
                userId = userId,
                messageType = messageType,
                messageId = messageId,
                haveRead = haveRead
            )
        }
        if (messageCount == 0) {
            return Page(
                page = page,
                pageSize = pageSize,
                count = 0,
                records = listOf()
            )
        }
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page = page, pageSize = pageSize)
        // 后台单独做项目级别的信息获取兼容
        val messageRecords = if (projectId != null) {
            streamUserMessageDao.getMessages(
                dslContext = dslContext,
                projectId = projectId,
                userId = triggerUserId,
                messageType = messageType,
                haveRead = haveRead,
                messageId = messageId,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )!!
        } else {
            streamUserMessageDao.getMessages(
                dslContext = dslContext,
                projectId = "",
                userId = userId,
                messageType = messageType,
                haveRead = haveRead,
                messageId = messageId,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )!!
        }

        val requestIds = messageRecords.map { it.messageId.toInt() }.toSet()
        val eventMap = getRequestMap(userId, gitProjectId, requestIds)
        val resultMap = mutableMapOf<String, MutableList<UserMessage>>()
        messageRecords.forEach { message ->
            val eventId = message.messageId.toLong()
            if (eventMap[eventId] == null) {
                return@forEach
            }
            val time = message.createTime.format(timeFormat)
            val content = eventMap[eventId]!!
            val failedNum = content.filter { it.triggerReasonName != TriggerReason.TRIGGER_SUCCESS.name }.size
            val userMassage = UserMessage(
                id = message.id,
                userId = message.userId,
                messageType = UserMessageType.valueOf(message.messageType),
                messageTitle = message.messageTitle,
                messageId = message.messageId,
                haveRead = message.haveRead,
                createTime = message.createTime.timestampmilli(),
                updateTime = message.updateTime.timestampmilli(),
                content = content,
                contentAttr = ContentAttr(
                    total = content.size,
                    failedNum = failedNum
                )
            )
            if (resultMap[time].isNullOrEmpty()) {
                resultMap[time] = mutableListOf(userMassage)
            } else {
                resultMap[time]!!.add(userMassage)
            }
        }
        return Page(
            page = page,
            pageSize = pageSize,
            count = messageCount.toLong(),
            records = resultMap.map { UserMessageRecord(time = it.key, records = it.value) }
        )
    }

    fun readMessage(
        userId: String,
        id: Int,
        projectCode: String?
    ): Boolean {
        websocketService.pushNotifyWebsocket(userId, projectCode)
        return streamUserMessageDao.readMessage(dslContext, id) >= 0
    }

    fun readAllMessage(
        projectCode: String?,
        userId: String
    ): Boolean {
        websocketService.pushNotifyWebsocket(userId, projectCode)
        return if (projectCode != null) {
            streamUserMessageDao.readAllMessage(
                dslContext = dslContext,
                projectCode = projectCode,
                userId = null
            ) >= 0
        } else {
            streamUserMessageDao.readAllMessage(
                dslContext = dslContext,
                userId = userId
            ) >= 0
        }
    }

    fun getNoReadMessageCount(
        projectId: String?,
        userId: String
    ): Int {
        return if (projectId != null) {
            streamUserMessageDao.getNoReadCount(
                dslContext = dslContext,
                projectId = projectId,
                userId = null
            )
        } else {
            streamUserMessageDao.getNoReadCount(
                dslContext = dslContext,
                projectId = "",
                userId = userId
            )
        }
    }

    private fun getRequestMap(
        userId: String,
        gitProjectId: Long?,
        requestIds: Set<Int>
    ): Map<Long, List<RequestMessageContent>> {
        val eventList = gitRequestEventDao.getRequestsById(
            dslContext = dslContext,
            requestIds = requestIds,
            hasEvent = true
        )
        if (eventList.isEmpty()) {
            return emptyMap()
        }
        val eventIds = eventList.map { it.id!! }.toSet()
        val resultMap = mutableMapOf<Long, List<RequestMessageContent>>()

        // 未触发的所有记录
        val noBuildList = gitRequestEventNotBuildDao.getListByEventIds(dslContext, eventIds)
        val noBuildMap = getNoBuildEventMap(noBuildList)
        // 触发的所有记录
        val buildList = gitRequestEventBuildDao.getByEventIds(dslContext, eventIds)
        val buildMap = getBuildEventMap(buildList)
        // 项目ID可能为空，用所有的构建记录的流水线ID查询流水线
        val pipelineMap = if (gitProjectId != null) {
            pipelineResourceDao.getPipelines(dslContext, gitProjectId).associateBy { it.pipelineId }
        } else {
            val pipelineIds =
                (noBuildList.map { it.pipelineId }.toSet() + buildList.map { it.pipelineId }.toSet()).toList()
            pipelineResourceDao.getPipelinesInIds(dslContext, null, pipelineIds).associateBy { it.pipelineId }
        }

        val processBuildList = client.get(ServiceBuildResource::class)
            .getBatchBuildStatus(
                projectId = GitCommonUtils.getCiProjectId(gitProjectId, streamGitConfig.getScmType()),
                buildId = buildList.map { it.buildId }.toSet(),
                channelCode = channelCode
            ).data?.associateBy { it.id }

        eventList.forEach { event ->
            val eventId = event.id!!
            val records = mutableListOf<RequestMessageContent>()
            // 添加未构建的记录
            noBuildMap[eventId]?.forEach nextBuild@{
                if (gitProjectId != null && it.gitProjectId != gitProjectId) {
                    return@nextBuild
                }
                val pipeline = if (it.pipelineId.isNullOrBlank()) {
                    null
                } else {
                    pipelineMap[it.pipelineId]
                }
                records.add(
                    RequestMessageContent(
                        id = it.id,
                        pipelineName = pipeline?.displayName ?: it.filePath,
                        buildBum = null,
                        triggerReasonName = it.reason,
                        triggerReasonDetail = it.reasonDetail,
                        filePathUrl = getYamlUrl(event, pipeline?.filePath)
                    )
                )
            }
            // 添加构建的记录
            buildMap[eventId]?.forEach nextBuild@{
                if (gitProjectId != null && it.gitProjectId != gitProjectId) {
                    return@nextBuild
                }
                val buildNum = processBuildList?.get(it.buildId)?.buildNum
                val pipeline = pipelineMap[it.pipelineId]
                records.add(
                    RequestMessageContent(
                        id = it.id,
                        pipelineName = pipeline?.displayName,
                        buildBum = buildNum,
                        triggerReasonName = TriggerReason.TRIGGER_SUCCESS.name,
                        triggerReasonDetail = null,
                        filePathUrl = getYamlUrl(event, pipeline?.filePath)
                    )
                )
            }
            resultMap[eventId] = records
        }
        return resultMap
    }

    private fun getBuildEventMap(
        buildList: List<TGitRequestEventBuildRecord>
    ): MutableMap<Long, MutableList<TGitRequestEventBuildRecord>> {
        val resultMap = mutableMapOf<Long, MutableList<TGitRequestEventBuildRecord>>()
        buildList.forEach {
            if (resultMap[it.eventId].isNullOrEmpty()) {
                resultMap[it.eventId] = mutableListOf(it)
            } else {
                resultMap[it.eventId]!!.add(it)
            }
        }
        return resultMap
    }

    private fun getNoBuildEventMap(
        noBuildList: List<TGitRequestEventNotBuildRecord>
    ): MutableMap<Long, MutableList<TGitRequestEventNotBuildRecord>> {
        val resultMap = mutableMapOf<Long, MutableList<TGitRequestEventNotBuildRecord>>()
        noBuildList.forEach {
            if (resultMap[it.eventId].isNullOrEmpty()) {
                resultMap[it.eventId] = mutableListOf(it)
            } else {
                resultMap[it.eventId]!!.add(it)
            }
        }
        return resultMap
    }

    private fun getYamlUrl(event: GitRequestEvent, filePath: String?): String? {

        val gitEvent = try {
            actionFactory.loadEvent(
                event.event,
                streamGitConfig.getScmType(),
                event.objectKind
            )
        } catch (e: Exception) {
            logger.warn("StreamUserMessageService|getYamlUrl|error", e)
            return null
        }
        if (filePath == null) {
            return null
        }

        val homepageAndBranch = when (gitEvent) {
            is GitPushEvent -> {
                Pair(gitEvent.repository.homepage, getTriggerBranch(gitEvent.ref))
            }
            is GitTagPushEvent -> {
                Pair(gitEvent.repository.homepage, getTriggerBranch(gitEvent.ref))
            }
            is GitMergeRequestEvent -> {
                Pair(
                    gitEvent.object_attributes.source.web_url,
                    getTriggerBranch(gitEvent.object_attributes.source_branch)
                )
            }
            is GithubPushEvent -> {
                Pair(gitEvent.repository.url, getTriggerBranch(gitEvent.ref))
            }
            is GithubPullRequestEvent -> {
                Pair(gitEvent.pullRequest.head.repo.url, getTriggerBranch(gitEvent.pullRequest.head.ref))
            }
            else -> {
                null
            }
        }
        return if (homepageAndBranch == null) {
            null
        } else {
            "${homepageAndBranch.first}/blob/${homepageAndBranch.second}/$filePath"
        }
    }

    private fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") ->
                branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") ->
                branch.removePrefix("refs/tags/")
            else -> branch
        }
    }
}
