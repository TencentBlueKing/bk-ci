package com.tencent.devops.process.trigger

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.EVENT_REPLAY_DESC
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_DETAIL_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY
import com.tencent.devops.process.dao.PipelineTriggerEventDao
import com.tencent.devops.process.enums.BuildReplayStatus
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.BuildReplayResult
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.trigger.replay.TriggerReplayHandler
import com.tencent.devops.process.webhook.pojo.event.commit.ReplayWebhookEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PipelineTriggerReplayService(
    private val dslContext: DSLContext,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val triggerReplayHandlers: List<TriggerReplayHandler>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerReplayService::class.java)
    }

    /**
     * 重放单条流水线，异步触发
     * @return 重放生成的事件ID
     */
    fun replay(
        userId: String,
        projectId: String,
        detailId: Long
    ): Long {
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
        val triggerEvent = getTriggerEvent(projectId = projectId, eventId = triggerDetail.eventId)
        val handler = getReplayHandler(triggerEvent)
        val (replayEvent, replayTriggerEvent) = saveReplayEvent(
            userId = userId,
            projectId = projectId,
            triggerEvent = triggerEvent,
            pipelineId = pipelineId
        )
        handler.replay(replayEvent = replayEvent, replayTriggerEvent = replayTriggerEvent)
        return replayEvent.eventId
    }

    /**
     * 重放事件下的全部流水线，异步触发
     * @return 重放生成的事件ID
     */
    fun replayAll(
        userId: String,
        projectId: String,
        eventId: Long
    ): Long {
        logger.info("replay all pipeline trigger event|$userId|$projectId|$eventId")
        val triggerEvent = getTriggerEvent(projectId = projectId, eventId = eventId)
        val handler = getReplayHandler(triggerEvent)
        val (replayEvent, replayTriggerEvent) = saveReplayEvent(
            userId = userId,
            projectId = projectId,
            triggerEvent = triggerEvent,
            pipelineId = null
        )
        handler.replay(replayEvent = replayEvent, replayTriggerEvent = replayTriggerEvent)
        return replayEvent.eventId
    }

    /**
     * 重新构建，同步按最新版本触发，流水线执行权限由调用方校验
     */
    fun replayByBuild(
        userId: String,
        projectId: String,
        triggerDetail: PipelineTriggerDetail
    ): BuildReplayResult {
        logger.info("replay by build|$userId|$projectId|${triggerDetail.detailId}")
        val pipelineId = triggerDetail.pipelineId ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY,
            params = arrayOf(triggerDetail.detailId.toString())
        )
        val triggerEvent = getTriggerEvent(projectId = projectId, eventId = triggerDetail.eventId)
        val handler = getReplayHandler(triggerEvent)
        val (replayEvent, replayTriggerEvent) = saveReplayEvent(
            userId = userId,
            projectId = projectId,
            triggerEvent = triggerEvent,
            pipelineId = pipelineId
        )
        val buildId = handler.replayBuild(replayEvent = replayEvent, replayTriggerEvent = replayTriggerEvent)
        return if (buildId.isNullOrBlank()) {
            BuildReplayResult(status = BuildReplayStatus.REPLAY_FAILED, eventId = replayEvent.eventId)
        } else {
            BuildReplayResult(status = BuildReplayStatus.REPLAY_SUCCESS, id = buildId, eventId = replayEvent.eventId)
        }
    }

    private fun getTriggerEvent(projectId: String, eventId: Long): PipelineTriggerEvent {
        return pipelineTriggerEventDao.getTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
            params = arrayOf(eventId.toString())
        )
    }

    private fun getReplayHandler(triggerEvent: PipelineTriggerEvent): TriggerReplayHandler {
        val triggerType = PipelineTriggerType.parse(triggerEvent.triggerType)
        return triggerType?.let { type -> triggerReplayHandlers.find { it.support(type) } }
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TRIGGER_TYPE_REPLAY_NOT_SUPPORT,
                params = arrayOf(triggerEvent.triggerType)
            )
    }

    /**
     * 保存重放生成的触发事件，调用前需已通过 getReplayHandler 校验触发类型
     */
    private fun saveReplayEvent(
        userId: String,
        projectId: String,
        triggerEvent: PipelineTriggerEvent,
        pipelineId: String?
    ): Pair<ReplayWebhookEvent, PipelineTriggerEvent> {
        val sourceEventId = triggerEvent.eventId
        val triggerType = PipelineTriggerType.parse(triggerEvent.triggerType)!!
        val requestId = MDC.get(TraceTag.BIZID)
        val replayEventId = pipelineTriggerEventService.getEventId()
        // 如果重试的事件也由重试产生,则应该记录最开始的请求ID
        val replayRequestId = triggerEvent.replayRequestId ?: triggerEvent.requestId
        val replayTriggerEvent = with(triggerEvent) {
            PipelineTriggerEvent(
                requestId = requestId,
                projectId = projectId,
                eventId = replayEventId,
                triggerType = triggerType.name,
                eventSource = eventSource,
                eventType = eventType,
                triggerUser = userId,
                eventDesc = I18Variable(
                    code = EVENT_REPLAY_DESC,
                    params = listOf(sourceEventId.toString(), userId)
                ).toJsonStr(),
                replayRequestId = replayRequestId,
                requestParams = requestParams,
                createTime = LocalDateTime.now(),
                eventBody = triggerEvent.eventBody
            )
        }
        pipelineTriggerEventDao.save(
            dslContext = dslContext,
            triggerEvent = replayTriggerEvent
        )
        val replayEvent = ReplayWebhookEvent(
            userId = userId,
            projectId = projectId,
            eventId = replayEventId,
            replayRequestId = replayRequestId,
            scmType = triggerType.toScmType(),
            triggerType = triggerType.name,
            pipelineId = pipelineId
        )
        return Pair(replayEvent, replayTriggerEvent)
    }
}
