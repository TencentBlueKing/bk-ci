package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants.TRIGGER_CONDITION_NOT_MATCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_TRIGGER_EVENT_TYPE
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_ID
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_IP
import com.tencent.devops.process.constant.PipelineBuildParamKey.CI_NODE_NAME
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.trigger.MarketEventStartRequest
import com.tencent.devops.process.pojo.trigger.PipelineTriggerFailedMatchElement
import com.tencent.devops.process.service.CreateStreamTriggerSupportService
import com.tencent.devops.process.trigger.PipelineTriggerEventService
import com.tencent.devops.process.trigger.WebhookTriggerBuildService
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.event.CdsWebhookTriggerEvent
import com.tencent.devops.process.trigger.event.GenericWebhookTriggerEvent
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerContext
import com.tencent.devops.process.trigger.scm.listener.WebhookTriggerManager
import com.tencent.devops.process.utils.NODE_AGENT_ID
import com.tencent.devops.process.utils.PIPELINE_START_TRIGGER_EVENT_USER_ID
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
    private val webhookTriggerBuildService: WebhookTriggerBuildService,
    private val creativeStreamService: CreateStreamTriggerSupportService
) {

    fun cdsWebhookTrigger(event: CdsWebhookTriggerEvent) {
        // :TODO 后续启动参数key改常量
        with(event) {
            genericWebhookTrigger(
                GenericWebhookTriggerEvent(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    eventId = eventId,
                    version = null,
                    eventCode = eventCode,
                    eventSource = envHashId,
                    requestTime = requestTime,
                    extStartParam = event.startParams()
                )
            )
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    fun genericWebhookTrigger(event: GenericWebhookTriggerEvent) {
        logger.info("receive generic webhook request event[${JsonUtil.toJson(event, false)}]")
        with(event) {
            val context = WebhookTriggerContext(
                projectId = projectId,
                pipelineId = pipelineId,
                eventId = eventId,
                startType = StartType.TRIGGER_EVENT
            )
            try {
                val triggerEvent = pipelineTriggerEventService.getTriggerEvent(
                    projectId = projectId, eventId = eventId
                ) ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
                    params = arrayOf(eventId.toString())
                )
                val triggerEventBody = triggerEvent.eventBody ?: throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_BODY_NOT_FOUND,
                    params = arrayOf(eventId.toString())
                )
                val pipelineInfo =
                    pipelineRepositoryService.getPipelineInfo(projectId, pipelineId) ?: throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                        params = arrayOf(pipelineId)
                    )
                if (pipelineInfo.locked == true) return
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
                triggerContainer.elements.filterIsInstance<MarketEventAtomElement>().forEach elements@{ element ->
                    if (!element.elementEnabled() || element.atomCode != eventCode) {
                        return@elements
                    }

                    val atomResponse = marketEventTriggerMatcher.matches(
                        projectId = projectId,
                        pipelineId = pipelineId,
                        triggerEventBody = triggerEventBody,
                        variables = variables,
                        element = element,
                        extStartParam = extStartParam ?: mapOf() // 扩展变量，记录业务侧填充的内置变量
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

    @Suppress("NestedBlockDepth")
    fun openapiTrigger(
        userId: String,
        projectId: String,
        pipelineId: String,
        eventCode: String,
        request: MarketEventStartRequest
    ): BuildId {
        logger.info(
            "receive market event trigger request|$projectId|$pipelineId|$eventCode"
        )
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId, pipelineId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            params = arrayOf(pipelineId)
        )
        if (pipelineInfo.locked == true) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_LOCK,
                params = arrayOf(pipelineId)
            )
        }
        val resource = pipelineRepositoryService.getPipelineResourceVersion(
            projectId, pipelineId, null
        ) ?: throw ErrorCodeException(
            statusCode = Response.Status.NOT_FOUND.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NOT_EXISTS
        )
        val model = resource.model
        val triggerContainer = model.getTriggerContainer()
        // 传入的启动参数,替换成流水线默认值
        val variables = pipelineRepositoryService.getTriggerParams(
            triggerContainer = triggerContainer,
            startParams = request.startParams
        )
        // 额外获取创作流的启动参数
        val extStartParam = resolveCreativeStreamParams(
            pipelineInfo = pipelineInfo,
            userId = userId
        )
        val failedMatchElements = mutableListOf<PipelineTriggerFailedMatchElement>()
        triggerContainer.elements
            .filterIsInstance<MarketEventAtomElement>()
            .filter { it.elementEnabled() && it.atomCode == eventCode }
            .also { elements ->
                if (elements.isEmpty()) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_TRIGGER_CONDITION_NOT_MATCH,
                        params = arrayOf(eventCode)
                    )
                }
            }
            .forEach elements@{ element ->
                val atomResponse = marketEventTriggerMatcher.matches(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    triggerEventBody = request.eventBody,
                    variables = variables,
                    element = element,
                    extStartParam = extStartParam
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
                        return webhookTriggerBuildService.startPipeline(
                            pipelineInfo = pipelineInfo,
                            resource = resource,
                            startParams = atomResponse.outputVars,
                            startType = StartType.TRIGGER_EVENT
                        )
                    }

                    else -> return@elements
                }
            }
        throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_CONDITION_NOT_MATCH,
            params = arrayOf(
                failedMatchElements.joinToString("; ") {
                    "${it.elementName}: ${it.reasonMsg}"
                }
            )
        )
    }

    /**
     * 创作流流水线随机获取环境节点参数
     */
    private fun resolveCreativeStreamParams(
        pipelineInfo: com.tencent.devops.process.engine.pojo.PipelineInfo,
        userId: String
    ): Map<String, String> {
        if (pipelineInfo.channelCode != ChannelCode.CREATIVE_STREAM) {
            return emptyMap()
        }
        val setting = pipelineRepositoryService.getSetting(
            projectId = pipelineInfo.projectId,
            pipelineId = pipelineInfo.pipelineId
        )
        val envHashId = setting?.envHashId
        if (envHashId.isNullOrBlank()) {
            logger.warn(
                "creative stream pipeline has no envHashId|" +
                    "${pipelineInfo.projectId}|${pipelineInfo.pipelineId}"
            )
            return emptyMap()
        }
        val nodeList = creativeStreamService.getEnvNodeList(
            userId = userId,
            projectId = pipelineInfo.projectId,
            envHashId = envHashId
        ).filter { it.isNotBlank() }
        if (nodeList.isEmpty()) {
            logger.warn(
                "creative stream env node list is empty|" +
                    "${pipelineInfo.projectId}|$envHashId"
            )
            return emptyMap()
        }
        val agentHashId = nodeList.random()
        return creativeStreamService.creativeStreamParams(
            projectId = pipelineInfo.projectId,
            agentHashId = agentHashId,
            userId = userId
        )
    }

    /**
     * 事件触发启动参数
     */
    private fun CdsWebhookTriggerEvent.startParams(): Map<String, String> {
        return mutableMapOf(
            NODE_AGENT_ID to this.agentHashId,
            PIPELINE_START_TRIGGER_EVENT_USER_ID to userId, // 触发用户
            PIPELINE_TRIGGER_EVENT_TYPE to eventCode, // 记录事件标识，后续构建历史页面需根据事件标识过滤构建任务
            CI_NODE_ID to workspaceName, // 云桌面ID (ins-xxx)
            CI_NODE_IP to cdsIp, // 云桌面IP
            CI_NODE_NAME to cdsName // 云桌面名称
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventTriggerBuildService::class.java)
    }
}
