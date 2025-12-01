package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.event.GenericWebhookTriggerEvent
import com.tencent.devops.process.trigger.event.RemoteDevWebhookTriggerEvent
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 研发商店事件触发构建服务
 */
@Service
class MarketEventTriggerBuildService @Autowired constructor(
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val webhookTriggerManager: WebhookTriggerManager,
    private val marketEventTriggerMatcher: MarketEventTriggerMatcher,
    private val pipelineTriggerEventService: PipelineTriggerEventService,
    private val webhookTriggerBuildService: WebhookTriggerBuildService
) {

    fun remoteDevWebhookTrigger(event: RemoteDevWebhookTriggerEvent) {
        with(event) {
            genericWebhookTrigger(
                GenericWebhookTriggerEvent(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    eventId = eventId,
                    version = null,
                    eventCode = eventCode,
                    eventSource = envHashId,
                    requestTime = requestTime
                )
            )
        }
    }

    fun genericWebhookTrigger(event: GenericWebhookTriggerEvent) {
        with(event) {
            val context = WebhookTriggerContext(projectId = projectId, pipelineId = pipelineId, eventId = eventId)
            try {
                val triggerEvent = pipelineTriggerEventService.getTriggerEvent(
                    projectId = projectId, eventId = eventId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
                    params = arrayOf(eventId.toString())
                )
                val webhookRequest = triggerEvent.eventBody?.let {
                    JsonUtil.to(it, WebhookRequest::class.java)
                } ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_BODY_NOT_FOUND,
                    params = arrayOf(eventId.toString())
                )
                val pipelineInfo =
                    pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                        params = arrayOf(pipelineId)
                    )
                context.pipelineInfo = pipelineInfo

                val resource = pipelineRepositoryService.getPipelineResourceVersion(projectId, pipelineId, version)
                    ?: throw ErrorCodeException(
                        statusCode = Response.Status.NOT_FOUND.statusCode,
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
                    )
                val model = resource.model
                val triggerContainer = model.getTriggerContainer()
                val variables = pipelineRepositoryService.getTriggerParams(model.getTriggerContainer())
                val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
                triggerContainer.elements.filterIsInstance<MarketBuildLessAtomElement>().forEach elements@{ element ->
                    if (!element.elementEnabled() || element.atomCode != eventCode) {
                        return@elements
                    }

                    val atomResponse = marketEventTriggerMatcher.matches(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        webhookRequest = webhookRequest,
                        variables = variables,
                        element = element
                    )
                    when (atomResponse.matchStatus) {
                        MatchStatus.CONDITION_NOT_MATCH -> {
                            failedMatchElements.add(
                                PipelineTriggerFailedMatchElement(
                                    elementId = element.id,
                                    elementName = element.name,
                                    elementAtomCode = element.getAtomCode(),
                                    reasonMsg = atomResponse.failedReason ?: I18Variable(
                                        code = TRIGGER_CONDITION_NOT_MATCH
                                    ).toJsonStr()
                                )
                            )
                        }

                        MatchStatus.SUCCESS -> {
                            webhookTriggerBuildService.startPipeline(
                                context = context,
                                pipelineInfo = pipelineInfo,
                                resource = resource,
                                startParams = atomResponse.outputVars
                            )
                            return
                        }

                        else -> {
                            return@elements
                        }
                    }
                }
                if (failedMatchElements.isNotEmpty()) {
                    context.failedMatchElements = failedMatchElements
                    webhookTriggerManager.fireMatchFailed(context)
                }
            } catch (ignored: Exception) {
                logger.error(
                    "Failed to trigger by webhook|$projectId|$pipelineId|$eventSource",
                    ignored
                )
                webhookTriggerManager.fireError(context, ignored)
            }
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventTriggerBuildService::class.java)
    }
}
