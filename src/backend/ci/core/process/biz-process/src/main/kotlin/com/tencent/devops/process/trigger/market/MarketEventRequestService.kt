package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REMOTE_DEV_TRIGGER_DESC
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.GenericWebhookRequestEvent
import com.tencent.devops.process.trigger.event.RemoteDevWebhookRequestEvent
import com.tencent.devops.process.trigger.event.RemoteDevWebhookTriggerEvent
import org.jooq.DSLContext
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 研发商店事件管理请求服务
 */
@Service
class MarketEventRequestService constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao
) {
    fun handleRemoteDevWebhookRequestEvent(event: RemoteDevWebhookRequestEvent) {
        with(event) {
            // 1. 获取事件源: 通过项目ID+workspaceName获取环境列表
            client.get(ServiceEnvironmentResource::class).listRawByEnvNames(
                projectId = projectId,
                envNames = listOf(workspaceName),
                userId = userId
            )
            val envIdList = listOf("env_1", "env_2")
            val eventDesc = JsonUtil.toJson(
                I18Variable(
                    code = BK_REMOTE_DEV_TRIGGER_DESC,
                    params = listOf(cdsIp, eventType)
                ),
                false
            )
            val requestId = MDC.get(TraceTag.BIZID)
            val requestTime = System.currentTimeMillis()
            envIdList.forEach { envHashId ->
                val eventId = pipelineTriggerEventService.getEventId()
                val triggerEvent = PipelineTriggerEvent(
                    projectId = projectId,
                    eventId = eventId,
                    triggerType = StartType.WEB_HOOK.name,
                    eventSource = envHashId,
                    eventType = eventType,
                    triggerUser = userId,
                    eventDesc = eventDesc,
                    requestId = requestId,
                    createTime = LocalDateTime.now(),
                    eventBody = event.body
                )
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
                // 2. 获取事件订阅者
                val subscribers = pipelineEventSubscriptionDao.listEventSubscriber(
                    dslContext = dslContext,
                    eventType = eventType,
                    eventSource = envHashId,
                    eventCode = eventCode
                )
                subscribers.forEach { subscriber ->
                    sampleEventDispatcher.dispatch(
                        RemoteDevWebhookTriggerEvent(
                            userId = userId,
                            projectId = projectId,
                            pipelineId = subscriber.pipelineId,
                            workspaceName = workspaceName,
                            cdsIp = cdsIp,
                            eventCode= eventCode,
                            eventId = eventId,
                            envHashId = envHashId,
                            requestTime = requestTime,
                        )
                    )
                }
            }
        }

    }

    fun handleGenericWebhookRequestEvent(event: GenericWebhookRequestEvent) {
        // 1. 查询eventCode对应的提供者信息
        // 2. 提取事件源和事件类型
    }
}
