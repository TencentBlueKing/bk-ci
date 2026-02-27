package com.tencent.devops.process.trigger.market

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_ENV_AGENT_HASH_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_EVENT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_WORKSPACE_NAME
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REMOTE_DEV_TRIGGER_DESC
import com.tencent.devops.process.pojo.WorkspaceBaseInfo
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.service.CreativeStreamService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.GenericWebhookRequestEvent
import com.tencent.devops.process.trigger.event.CdsWebhookRequestEvent
import com.tencent.devops.process.trigger.event.CdsWebhookTriggerEvent
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 研发商店事件管理请求服务
 */
@Service
class MarketEventRequestService constructor(
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val creativeStreamService: CreativeStreamService
) {
    fun handleCdsWebhookRequestEvent(event: CdsWebhookRequestEvent) {
        logger.info("Receive CdsWebhookRequestEvent from MQ [$event]")
        with(event) {
            // 1. 获取事件源: 通过项目ID+workspaceName获取环境列表
            val envList = creativeStreamService.fetchAllNodeEnvList(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId
            )
            if (envList.isEmpty()) {
                logger.warn("target env list is empty|$projectId|$workspaceName")
                return
            }
            // 云桌面信息
            val workspaceInfo = creativeStreamService.getWorkspaceInfoByName(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId
            )
            val eventDesc = I18Variable(
                code = BK_REMOTE_DEV_TRIGGER_DESC,
                params = listOf(workspaceInfo?.displayName ?: workspaceName, userId, eventType)
            ).toJsonStr()
            val requestId = MDC.get(TraceTag.BIZID)
            envList.forEach { env ->
                val eventId = pipelineTriggerEventService.getEventId()
                val triggerEvent = PipelineTriggerEvent(
                    projectId = projectId,
                    eventId = eventId,
                    triggerType = StartType.TRIGGER_EVENT.name,
                    eventSource = env.hashId,
                    eventType = eventCode, // 记录具体事件标识，后续用于【触发事件】过滤
                    triggerUser = userId,
                    eventDesc = eventDesc,
                    requestId = requestId,
                    createTime = LocalDateTime.now(),
                    eventBody = GenericWebhookEventBody(
                        headers = mapOf(
                            AUTH_HEADER_WORKSPACE_NAME to workspaceName,
                            AUTH_HEADER_CDS_IP to cdsIp,
                            AUTH_HEADER_EVENT_TYPE to eventType,
                            AUTH_HEADER_USER_ID to userId,
                            AUTH_HEADER_ENV_AGENT_HASH_ID to env.agentHashId
                        ),
                        body = event.body.let {
                            if (it.isBlank()) {
                                JsonUtil.anyToOrNull(
                                    it,
                                    object : TypeReference<Map<String, String>>() {}
                                )
                            } else {
                                mapOf()
                            }
                        },
                        queryParams = mapOf()
                    )
                )
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
                // 2. 使用公共方法处理事件分发
                handleTriggerEvent(
                    triggerEvent = triggerEvent,
                    pipelineId = null
                )
            }
        }
    }

    /**
     * 处理PipelineTriggerEvent并分发CdsWebhookTriggerEvent
     * 用于处理研发商店事件的公共逻辑
     */
    @SuppressWarnings("NestedBlockDepth")
    fun handleTriggerEvent(
        triggerEvent: PipelineTriggerEvent,
        pipelineId: String? = null
    ) {
        // 1. 从triggerEvent中提取事件数据
        val eventBody = triggerEvent.eventBody as? GenericWebhookEventBody ?: run {
            logger.info("triggerEvent eventBody is not GenericWebhookEventBody, skip event handling")
            return
        }

        // 2. 从headers中提取必要信息
        val headers = eventBody.headers ?: emptyMap()
        val workspaceName = headers[AUTH_HEADER_WORKSPACE_NAME] ?: run {
            logger.info("workspaceName not found in headers, skip event handling")
            return
        }
        val cdsIp = headers[AUTH_HEADER_CDS_IP] ?: run {
            logger.info("cdsIp not found in headers, skip event handling")
            return
        }
        val eventType = headers[AUTH_HEADER_EVENT_TYPE] ?: run {
            logger.info("eventType not found in headers, skip event handling")
            return
        }
        val userId = headers[AUTH_HEADER_USER_ID] ?: run {
            logger.info("userId not found in headers, skip event handling")
            return
        }
        val agentHashId = headers[AUTH_HEADER_ENV_AGENT_HASH_ID] ?: run {
            logger.info("agentHashId not found in headers, skip event handling")
            return
        }

        // 3. 获取事件订阅者
        val projectId = triggerEvent.projectId ?: ""
        val subscribers = pipelineId?.let {
            listOf(
                PipelineEventSubscriber(
                    pipelineId = it,
                    projectId = projectId,
                    channelCode = ChannelCode.CREATIVE_STREAM
                )
            )
        } ?: creativeStreamService.listEventSubscriber(
            eventType = eventType,
            eventSource = triggerEvent.eventSource ?: "",
            eventCode = triggerEvent.eventType
        )

        // 根据名称（ins-xxx）获取云桌面信息
        var workspaceBaseInfo: WorkspaceBaseInfo? = null
        workspaceBaseInfo = subscribers.asSequence().mapNotNull { subscriber ->
            val pipelineOAuthUser = creativeStreamService.getPipelineOAuthUser(
                projectId = subscriber.projectId,
                pipelineId = subscriber.pipelineId
            )
            if (pipelineOAuthUser.isNullOrBlank()) {
                null
            } else {
                creativeStreamService.getWorkspaceInfoByName(
                    projectId = projectId,
                    workspaceName = workspaceName,
                    userId = pipelineOAuthUser
                )
            }
        }.firstOrNull()
        if (workspaceBaseInfo == null) {
            logger.warn("cannot find workspace base info")
        }
        // 4. 分发CdsWebhookTriggerEvent
        subscribers.forEach subscriber@{ subscriber ->
            sampleEventDispatcher.dispatch(
                CdsWebhookTriggerEvent(
                    userId = userId,
                    projectId = projectId,
                    pipelineId = subscriber.pipelineId,
                    workspaceName = workspaceName,
                    cdsIp = cdsIp,
                    eventCode = triggerEvent.eventType,
                    eventId = triggerEvent.eventId ?: 0L,
                    envHashId = triggerEvent.eventSource ?: "",
                    requestTime = System.currentTimeMillis(),
                    agentHashId = agentHashId,
                    cdsName = workspaceBaseInfo?.displayName ?: ""
                )
            )
        }
    }

    fun handleGenericWebhookRequestEvent(event: GenericWebhookRequestEvent) {
        // 1. 查询eventCode对应的提供者信息
        // 2. 提取事件源和事件类型
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventRequestService::class.java)
    }
}