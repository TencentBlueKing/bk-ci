package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.auth.AUTH_HEADER_CDS_IP
import com.tencent.devops.common.api.auth.AUTH_HEADER_EVENT_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_WORKSPACE_NAME
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.pojo.EnvData
import com.tencent.devops.process.constant.ProcessMessageCode.BK_REMOTE_DEV_TRIGGER_DESC
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineEventSubscriber
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.event.GenericWebhookRequestEvent
import com.tencent.devops.process.trigger.event.CdsWebhookRequestEvent
import com.tencent.devops.process.trigger.event.CdsWebhookTriggerEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
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
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) {
    fun handleCdsWebhookRequestEvent(event: CdsWebhookRequestEvent) {
        with(event) {
            // 1. 获取事件源: 通过项目ID+workspaceName获取环境列表
            val envList = fetchAllNodeEnvList(
                projectId = projectId,
                workspaceName = workspaceName,
                userId = userId
            ).let {
                if (it.isNullOrEmpty()) {
                    logger.warn("get env list failed|$projectId|$workspaceName")
                    return
                } else {
                    it
                }
            }
            val eventDesc = I18Variable(
                code = BK_REMOTE_DEV_TRIGGER_DESC,
                params = listOf(cdsIp, userId, eventType)
            ).toJsonStr()
            val requestId = MDC.get(TraceTag.BIZID)
            envList.forEach { env ->
                val eventId = pipelineTriggerEventService.getEventId()
                val envHashId = ""
                val triggerEvent = PipelineTriggerEvent(
                    projectId = projectId,
                    eventId = eventId,
                    triggerType = StartType.TRIGGER_EVENT.name,
                    eventSource = envHashId,
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
                            AUTH_HEADER_USER_ID to userId
                        ),
                        body = event.body,
                        queryParams = mapOf()
                    )
                )
                pipelineTriggerEventService.saveTriggerEvent(triggerEvent = triggerEvent)
                // 2. 使用公共方法处理事件分发
                handleTriggerEvent(
                    triggerEvent = triggerEvent,
                    envList = envList,
                    pipelineId = null
                )
            }
        }
    }

    private fun fetchAllNodeEnvList(
        projectId: String,
        workspaceName: String,
        userId: String
    ): List<EnvData> {
        val envList = client.get(ServiceEnvironmentResource::class).fetchAllNodeEnvList(
            projectId = projectId,
            workspaceName = workspaceName,
            userId = userId
        ).data ?: run {
            logger.error("get env list failed|$projectId|$workspaceName")
            return listOf()
        }
        return envList
    }

    /**
     * 处理PipelineTriggerEvent并分发CdsWebhookTriggerEvent
     * 用于处理研发商店事件的公共逻辑
     */
    fun handleTriggerEvent(
        triggerEvent: PipelineTriggerEvent,
        envList: List<EnvData>? = null,
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

        val triggerEnvList = envList ?: fetchAllNodeEnvList(
            projectId = triggerEvent.projectId ?: "",
            workspaceName = workspaceName,
            userId = userId
        ) ?: listOf()

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
        } ?: pipelineEventSubscriptionDao.listEventSubscriber(
            dslContext = dslContext,
            eventType = eventType,
            eventSource = triggerEvent.eventSource ?: "",
            eventCode = triggerEvent.eventType
        )

        // 4. 分发CdsWebhookTriggerEvent
        triggerEnvList.forEach { env ->
            run {
                subscribers.forEach subscriber@{ subscriber ->
                    val agentHashId = ""
                    // 获取权限代持人
                    val pipelineOAuthUser = getPipelineOAuthUser(
                        projectId = projectId,
                        pipelineId = subscriber.pipelineId
                    ) ?: run { return@subscriber }
                    // 获取云桌面信息
                    val cdsName = getWorkspaceInfo(
                        projectId = projectId,
                        agentHashId = agentHashId,
                        userId = pipelineOAuthUser
                    )?.first ?: ""
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
                            cdsName = cdsName
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

    /**
     * 获取云桌面信息
     */
    private fun getWorkspaceInfo(
        projectId: String,
        agentHashId: String,
        userId: String
    ): Pair<String, String>? {
        // TODO 完善获取云桌面信息
        return null
    }

    /**
     * 获取云桌面信息
     */
    private fun getPipelineOAuthUser(
        projectId: String,
        pipelineId: String
    ) = try {
        pipelineRepositoryService.getPipelineOauthUser(projectId, pipelineId)
    } catch (ignored: Exception) {
        logger.warn("get pipeline oauth user failed", ignored)
        null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventRequestService::class.java)
    }
}