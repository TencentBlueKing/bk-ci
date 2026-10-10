package com.tencent.devops.process.trigger.artifact

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.trigger.ArtifactTriggerElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineEventSubscriptionDao
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.pojo.trigger.artifact.ArtifactEvent
import com.tencent.devops.process.service.CreateStreamTriggerSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.event.ArtifactWebhookTriggerEvent
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import jakarta.ws.rs.core.Response
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 制品到达事件触发构建服务
 *
 * 与 [ArtifactWebhookRequestService] 分工：
 * - RequestService：解析原始 webhook、保存触发事件、投递精简版路由事件；
 * - BuildService：接收路由事件后反查 eventBody 拿原始 body，完成匹配 -> 组装启动参数 -> 启动流水线。
 */
@Service
class ArtifactEventTriggerBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val artifactTriggerMatcher: ArtifactTriggerMatcher,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val createStreamTriggerSupportService: CreateStreamTriggerSupportService,
    private val pipelineEventSubscriptionDao: PipelineEventSubscriptionDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactEventTriggerBuildService::class.java)
    }

    fun artifactWebhookTrigger(event: ArtifactWebhookTriggerEvent) {
        logger.info("Receive artifact webhook trigger event[${JsonUtil.toJson(event, false)}]")
        val context = WebhookTriggerContext(
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            eventId = event.eventId,
            startType = StartType.WEB_HOOK
        )
        try {
            doTrigger(event, context)
        } catch (ignored: Exception) {
            logger.warn(
                "Failed to trigger by artifact webhook|${event.projectId}|${event.pipelineId}|${event.eventSource}",
                ignored
            )
            webhookTriggerManager.fireError(context, ignored)
        }
    }

    private fun doTrigger(event: ArtifactWebhookTriggerEvent, context: WebhookTriggerContext) = with(event) {
        val triggerEvent = pipelineTriggerEventService.getTriggerEvent(projectId, eventId) ?: run {
            logger.info("artifact trigger event not found|$eventId|$projectId|$pipelineId")
            return@with
        }
        val eventBody = triggerEvent.eventBody as? GenericWebhookEventBody ?: run {
            logger.info("artifact trigger event body is empty|$eventId|$projectId|$pipelineId")
            return@with
        }
        val artifactEvent = eventBody.parseBody<ArtifactEvent>() ?: run {
            logger.warn("fail to parse artifact event body|$eventId|$projectId|$pipelineId")
            return@with
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                params = arrayOf(pipelineId)
            )
        context.pipelineInfo = pipelineInfo
        // 锁定时抛异常，由外层 catch 走 fireError 记录触发事件，避免静默丢弃
        if (pipelineInfo.locked == true) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK,
                params = arrayOf(pipelineId)
            )
        }

        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId = projectId,
            pipelineId = pipelineId,
            version = null
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )

        matchAndStart(
            event = this,
            context = context,
            pipelineInfo = pipelineInfo,
            resource = resource,
            artifactEvent = artifactEvent
        )
    }

    @Suppress("LongParameterList")
    private fun matchAndStart(
        event: ArtifactWebhookTriggerEvent,
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        artifactEvent: ArtifactEvent
    ) {
        // 下发事件仅按 pipelineId 去重丢失了 taskId，这里按订阅表精确圈定订阅了本次事件的触发器，
        // 避免同一流水线混配多仓库触发器时对不相关触发器做匹配
        val eventScopes = ArtifactWebhookUtils.buildQueryScopes(artifactEvent)
        val subscribedTaskIds = pipelineEventSubscriptionDao.listSubscribedTaskIds(
            dslContext = dslContext,
            projectId = event.projectId,
            pipelineId = event.pipelineId,
            eventSource = event.eventSource,
            eventType = event.eventType,
            eventCode = ArtifactTriggerElement.classType,
            eventScopes = eventScopes
        )
        val elements = resource.model.getTriggerContainer().elements
            .filterIsInstance<ArtifactTriggerElement>()
            .filter { it.elementEnabled() && it.id in subscribedTaskIds }
        if (elements.isEmpty()) {
            logger.info("no enabled artifact trigger element|${event.projectId}|${event.pipelineId}")
            return
        }
        val variables = pipelineRepositoryService.getTriggerParams(resource.model.getTriggerContainer())
        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        for (element in elements) {
            val atomResponse = artifactTriggerMatcher.matches(
                projectId = event.projectId,
                element = element,
                event = artifactEvent,
                pipelineId = event.pipelineId,
                variables = variables
            )
            when (atomResponse.matchStatus) {
                MatchStatus.SUCCESS -> {
                    startPipeline(
                        event = event,
                        context = context,
                        pipelineInfo = pipelineInfo,
                        resource = resource,
                        startParams = atomResponse.outputVars
                    )
                    logger.info(
                        "artifact webhook trigger success|${event.projectId}|${event.pipelineId}|${element.id}"
                    )
                    return
                }

                MatchStatus.CONDITION_NOT_MATCH -> failedMatchElements.add(
                    PipelineTriggerFailedMatchElement(
                        elementId = element.id,
                        elementName = element.name,
                        elementAtomCode = element.getAtomCode(),
                        reasonMsg = atomResponse.failedReason
                            ?: I18Variable(code = TRIGGER_CONDITION_NOT_MATCH).toJsonStr()
                    )
                )

                else -> Unit
            }
        }
        if (failedMatchElements.isNotEmpty()) {
            context.failedMatchElements = failedMatchElements
            webhookTriggerManager.fireMatchFailed(context)
        }
    }

    private fun startPipeline(
        event: ArtifactWebhookTriggerEvent,
        context: WebhookTriggerContext,
        pipelineInfo: PipelineInfo,
        resource: PipelineResourceVersion,
        startParams: Map<String, Any>
    ) {
        val oauthUserId = pipelineRepositoryService.getPipelineOauthUser(
            projectId = event.projectId,
            pipelineId = event.pipelineId
        ) ?: pipelineInfo.lastModifyUser
        val externalStartParams = createStreamTriggerSupportService.externalWebhookStartParams(
            pipelineInfo = pipelineInfo,
            userId = oauthUserId
        )
        webhookTriggerBuildService.startPipeline(
            context = context,
            pipelineInfo = pipelineInfo,
            resource = resource,
            startParams = startParams + externalStartParams
        )
    }
}
