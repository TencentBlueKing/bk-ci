/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
 *
 */

package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.EVENT_REPLAY_DESC
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_DETAIL_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY
import com.tencent.devops.process.dao.PipelineTriggerEventDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReason
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonStatistics
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventVo
import com.tencent.devops.process.webhook.CodeWebhookEventDispatcher
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.repository.api.ServiceRepositoryPermissionResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.time.LocalDateTime

@Suppress("ALL")
@Service
class PipelineTriggerEventService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao,
    private val streamBridge: StreamBridge,
    private val pipelinePermissionService: PipelinePermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerEventService::class.java)
        private const val PIPELINE_TRIGGER_EVENT_BIZ_ID = "PIPELINE_TRIGGER_EVENT"
        private const val PIPELINE_TRIGGER_DETAIL_BIZ_ID = "PIPELINE_TRIGGER_DETAIL"
        // 构建链接
        const val PIPELINE_BUILD_URL_PATTERN = "<a href=\"{0}\" target=\"_blank\">#{1}</a>"
    }

    fun getDetailId(): Long {
        return client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_TRIGGER_DETAIL_BIZ_ID).data ?: 0
    }

    fun getEventId(): Long {
        return client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_TRIGGER_EVENT_BIZ_ID).data ?: 0
    }

    fun getEventId(projectId: String, requestId: String, eventSource: String): Long {
        return pipelineTriggerEventDao.getEventByRequestId(
            dslContext = dslContext,
            projectId = projectId,
            requestId = requestId,
            eventSource = eventSource
        )?.eventId ?: getEventId()
    }

    fun getEventIdOrNull(projectId: String, requestId: String, eventSource: String): Long? {
        return pipelineTriggerEventDao.getEventByRequestId(
            dslContext = dslContext,
            projectId = projectId,
            requestId = requestId,
            eventSource = eventSource
        )?.eventId
    }

    fun saveEvent(
        triggerEvent: PipelineTriggerEvent,
        triggerDetail: PipelineTriggerDetail
    ) {
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineTriggerEventDao.save(
                dslContext = transactionContext,
                triggerEvent = triggerEvent
            )
            pipelineTriggerEventDao.saveDetail(
                dslContext = transactionContext,
                triggerDetail = triggerDetail
            )
        }
    }

    fun saveTriggerEvent(triggerEvent: PipelineTriggerEvent) {
        pipelineTriggerEventDao.save(
            dslContext = dslContext,
            triggerEvent = triggerEvent
        )
    }

    fun updateTriggerEvent(triggerEvent: PipelineTriggerEvent) {
        pipelineTriggerEventDao.update(
            dslContext = dslContext,
            triggerEvent = triggerEvent
        )
    }

    fun saveTriggerDetail(triggerDetail: PipelineTriggerDetail) {
        pipelineTriggerEventDao.saveDetail(
            dslContext = dslContext,
            triggerDetail = triggerDetail
        )
    }

    fun getTriggerEvent(projectId: String, eventId: Long): PipelineTriggerEvent? {
        return pipelineTriggerEventDao.getTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        )
    }

    fun listPipelineTriggerEvent(
        userId: String,
        projectId: String,
        pipelineId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        eventId: Long?,
        reason: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<PipelineTriggerEventVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val count = pipelineTriggerEventDao.countTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            eventType = eventType,
            triggerType = triggerType,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            eventId = eventId,
            reason = reason,
            startTime = startTime,
            endTime = endTime
        )
        val records = pipelineTriggerEventDao.listTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            eventId = eventId,
            reason = reason,
            startTime = startTime,
            endTime = endTime,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            fillEventDetailParam(it)
        }
        return SQLPage(count = count, records = records)
    }

    fun listRepoTriggerEvent(
        projectId: String,
        repoHashId: String,
        triggerType: String?,
        eventType: String?,
        triggerUser: String?,
        pipelineId: String?,
        eventId: Long?,
        pipelineName: String?,
        reason: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?,
        userId: String
    ): SQLPage<RepoTriggerEventVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        // 仅查询Event表
        val queryEvent = pipelineName.isNullOrBlank() && pipelineId.isNullOrBlank() && reason == null
        // 事件ID to 总数
        val (eventIds, count) = if (queryEvent) {
            val eventIds = pipelineTriggerEventDao.getEventIdsByEvent(
                dslContext = dslContext,
                projectId = projectId,
                eventSource = repoHashId,
                eventId = eventId,
                eventType = eventType,
                triggerUser = triggerUser,
                triggerType = triggerType,
                startTime = startTime,
                endTime = endTime,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
            val count = pipelineTriggerEventDao.getCountByEvent(
                dslContext = dslContext,
                eventId = eventId,
                projectId = projectId,
                eventSource = repoHashId,
                eventType = eventType,
                triggerType = triggerType,
                triggerUser = triggerUser,
                startTime = startTime,
                endTime = endTime
            )
            eventIds to count
        } else {
            val eventIds = pipelineTriggerEventDao.getDetailEventIds(
                dslContext = dslContext,
                projectId = projectId,
                eventSource = repoHashId,
                eventId = eventId,
                eventType = eventType,
                reason = reason,
                triggerUser = triggerUser,
                triggerType = triggerType,
                startTime = startTime,
                endTime = endTime,
                pipelineName = pipelineName,
                pipelineId = pipelineId,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
            val count = pipelineTriggerEventDao.getCountByDetail(
                dslContext = dslContext,
                projectId = projectId,
                eventSource = repoHashId,
                eventId = eventId,
                eventType = eventType,
                reason = reason,
                triggerUser = triggerUser,
                triggerType = triggerType,
                startTime = startTime,
                endTime = endTime,
                pipelineName = pipelineName,
                pipelineId = pipelineId
            )
            eventIds to count
        }
        // 事件信息
        val triggerEvent = pipelineTriggerEventDao.listRepoTriggerEvent(
            dslContext = dslContext,
            eventIds = eventIds
        )
        // 触发详情记录（总数，成功数）
        val eventDetailsMap = pipelineTriggerEventDao.listRepoTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            eventIds = eventIds,
            pipelineName = pipelineName,
            pipelineId = pipelineId
        ).associateBy { it.eventId }
        val records = triggerEvent.map {
            RepoTriggerEventVo(
                projectId = it.projectId,
                eventId = it.eventId,
                repoHashId = it.eventSource,
                eventDesc = getI18nEventDesc(it.eventDesc),
                eventTime = it.createTime.timestampmilli(),
                total = eventDetailsMap[it.eventId]?.total ?: 0,
                success = eventDetailsMap[it.eventId]?.success ?: 0
            )
        }
        return SQLPage(count = count, records = records)
    }

    fun listRepoTriggerEventDetail(
        projectId: String,
        eventId: Long,
        pipelineId: String?,
        pipelineName: String?,
        reason: String?,
        page: Int?,
        pageSize: Int?,
        userId: String
    ): SQLPage<PipelineTriggerEventVo> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val records = pipelineTriggerEventDao.listTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            reason = reason,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            fillEventDetailParam(it)
        }
        val count = pipelineTriggerEventDao.countTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            pipelineId = pipelineId,
            pipelineName = pipelineName,
            reason = reason
        )
        return SQLPage(count = count, records = records)
    }

    fun replay(
        userId: String,
        projectId: String,
        detailId: Long
    ): Boolean {
        logger.info("replay pipeline trigger event|$userId|$projectId|$detailId")
        val triggerDetail = pipelineTriggerEventDao.getTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            detailId = detailId
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_DETAIL_NOT_FOUND,
            params = arrayOf(detailId.toString())
        )
        val pipelineId = triggerDetail.pipelineId ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY,
            params = arrayOf(detailId.toString())
        )
        val permission = AuthPermission.EXECUTE
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = permission,
            message = MessageUtil.getMessageByLocale(
                CommonMessageCode.USER_NOT_PERMISSIONS_OPERATE_PIPELINE,
                I18nUtil.getLanguage(userId),
                arrayOf(userId, projectId, permission.getI18n(I18nUtil.getLanguage(userId)), pipelineId)
            )
        )
        replayAll(
            userId = userId,
            projectId = projectId,
            eventId = triggerDetail.eventId,
            pipelineId = pipelineId
        )
        return true
    }

    fun replayAll(
        userId: String,
        projectId: String,
        eventId: Long,
        pipelineId: String? = null
    ): Boolean {
        logger.info("replay all pipeline trigger event|$userId|$projectId|$eventId")
        val triggerEvent = pipelineTriggerEventDao.getTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
            params = arrayOf(eventId.toString())
        )
        val scmType = PipelineTriggerType.toScmType(triggerEvent.triggerType) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_TYPE_REPLAY_NOT_SUPPORT,
            params = arrayOf(triggerEvent.triggerType)
        )
        client.get(ServiceRepositoryPermissionResource::class).validatePermission(
            userId = userId,
            projectId = projectId,
            repositoryHashId = triggerEvent.eventSource!!,
            permission = AuthPermission.USE
        )
        // 保存重放事件
        val requestId = MDC.get(TraceTag.BIZID)
        val replayEventId = getEventId()
        // 如果重试的事件也由重试产生,则应该记录最开始的请求ID
        val replayRequestId = triggerEvent.replayRequestId ?: triggerEvent.requestId
        val replayTriggerEvent = with(triggerEvent) {
            PipelineTriggerEvent(
                requestId = requestId,
                projectId = projectId,
                eventId = replayEventId,
                triggerType = triggerType,
                eventSource = eventSource,
                eventType = eventType,
                triggerUser = userId,
                eventDesc = I18Variable(
                    code = EVENT_REPLAY_DESC,
                    params = listOf(eventId.toString(), userId)
                ).toJsonStr(),
                replayRequestId = replayRequestId,
                requestParams = requestParams,
                createTime = LocalDateTime.now()
            )
        }
        pipelineTriggerEventDao.save(
            dslContext = dslContext,
            triggerEvent = replayTriggerEvent
        )
        CodeWebhookEventDispatcher.dispatchReplayEvent(
            streamBridge = streamBridge,
            event = ReplayWebhookEvent(
                userId = userId,
                projectId = projectId,
                eventId = replayEventId,
                replayRequestId = replayRequestId,
                scmType = scmType,
                pipelineId = pipelineId
            )
        )
        return true
    }

    fun triggerReasonStatistics(
        projectId: String,
        eventId: Long,
        pipelineId: String?,
        pipelineName: String?
    ): PipelineTriggerReasonStatistics {
        return pipelineTriggerEventDao.triggerReasonStatistics(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            pipelineId = pipelineId,
            pipelineName = pipelineName
        )
    }

    /**
     * 获取国际化构建事件描述
     */
    private fun getI18nEventDesc(eventDesc: String) = try {
        JsonUtil.to(eventDesc, I18Variable::class.java).getCodeLanMessage()
    } catch (ignored: Exception) {
        logger.warn("Failed to resolve repo trigger event|sourceDesc[$eventDesc]", ignored)
        eventDesc
    }

    private fun getI18nReason(reason: String?): String = getCodeLanMessage(
        messageCode = if (reason.isNullOrBlank()) {
            PipelineTriggerReason.TRIGGER_SUCCESS.name
        } else {
            reason
        },
        defaultMessage = reason
    )

    /**
     * 获取构建链接
     */
    private fun PipelineTriggerEventVo.getBuildNumUrl(): String? {
        return if (status == PipelineTriggerStatus.SUCCEED.name) {
            when {
                !buildId.isNullOrBlank() -> {
                    val linkUrl = "/console/pipeline/$projectId/$pipelineId/detail/$buildId/executeDetail"
                    MessageFormat.format(PIPELINE_BUILD_URL_PATTERN, linkUrl, buildNum)
                }

                !reasonDetailList.isNullOrEmpty() -> {
                    reasonDetailList!![0]
                }

                else ->
                    null
            }
        } else {
            null
        }
    }

    /**
     * 填充事件相关参数
     * 事件描述国际化,构建链接,失败详情国际化,触发状态国际化,失败状态国际化
     */
    private fun fillEventDetailParam(
        eventParam: PipelineTriggerEventVo
    ): PipelineTriggerEventVo {
        return with(eventParam) {
            eventDesc = getI18nEventDesc(eventDesc)
            buildNum = getBuildNumUrl()
            reason = getI18nReason(eventParam.reason)
            this
        }
    }
}
