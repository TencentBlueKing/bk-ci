package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_CONFIG
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerEventConfig
import com.tencent.devops.store.pojo.trigger.conditions.InputCondition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MarketEventTriggerMatcher @Autowired constructor(
    val client: Client,
    val marketEventVariablesResolver: MarketEventVariablesResolver
) {

    fun matches(
        projectId: String,
        pipelineId: String,
        webhookRequest: WebhookRequest,
        variables: Map<String, String>,
        element: MarketEventAtomElement
    ): WebhookAtomResponse {
        // 根据插件版本,获取事件字段映射和触发条件
        val atomCode = element.atomCode
        val version = element.version
        logger.info("start to match event trigger|$projectId|$pipelineId|$atomCode@$version|${element.id}")
        val componentDetail = client.get(ServiceStoreComponentResource::class).getComponentDetailInfoByCode(
            userId = "admin",
            storeType = StoreTypeEnum.TRIGGER_EVENT,
            storeCode = atomCode,
            version = version
        ).data ?: throw InvalidParamException("component[$atomCode@$version] not found")
        // 事件配置
        val triggerConditionMap = componentDetail.extData?.get(KEY_TRIGGER_EVENT_CONFIG) as Map<String, Any>?
        if (triggerConditionMap.isNullOrEmpty()) {
            throw InvalidParamException("trigger condition not found")
        }
        val eventConfig = JsonUtil.mapTo(triggerConditionMap, TriggerEventConfig::class.java)
        // 解析request获取映射字段的值
        val eventVariables = marketEventVariablesResolver.getEventVariables(
            fieldMappings = eventConfig.fieldMapping,
            incomingHeaders = webhookRequest.headers,
            incomingQueryParamMap = webhookRequest.queryParams,
            incomingBody = webhookRequest.body
        )
        // val fieldMap = eventConfig.fieldMapping.associate { it.sourcePath to it.targetField }
        // 计算匹配结果
        val matchResult = evaluate(
            projectId = projectId,
            pipelineId = pipelineId,
            element = element,
            variables = variables,
            eventConfig = eventConfig,
            eventVariables = eventVariables
        )
        return if (matchResult.isMatch) {
            // 生成启动参数
            val startParams = mutableMapOf<String, Any>()
            startParams.putAll(variables)
            startParams.putAll(eventVariables)
            WebhookAtomResponse(
                matchStatus = MatchStatus.SUCCESS,
                outputVars = startParams
            )
        } else {
            WebhookAtomResponse(
                matchStatus = MatchStatus.CONDITION_NOT_MATCH,
                failedReason = matchResult.reason
            )
        }
    }

    private fun evaluate(
        projectId: String,
        pipelineId: String,
        element: MarketEventAtomElement,
        variables: Map<String, String>,
        eventConfig: TriggerEventConfig,
        eventVariables: Map<String, Any>,
    ): WebhookMatchResult {
        eventConfig.conditions.forEach { condition ->
            // 触发变量值
            val eventValue = eventVariables[condition.targetField]
            val input = element.data[KEY_INPUT] as Map<String, Any>? ?: mapOf()
            // 目标变量值
            val inputValue = input[condition.key()]?.let {
                when (it) {
                    is String -> EnvUtils.parseEnv(it, variables)
                    is List<*> -> it.map { item ->
                        item as String
                        EnvUtils.parseEnv(item, variables)
                    }

                    else -> InvalidParamException("unsupported type of $it")
                }
            }
            val finalInputValue = when {
                // 输入框,需要判断是否是多选
                condition is InputCondition &&
                        condition.multiple == true &&
                        condition.separator != null ->
                    inputValue?.toString()?.split(condition.separator!!)

                else -> inputValue
            }
            val expression = condition.operator.expression
            val result = expression.evaluate(eventValue, finalInputValue)
            logger.info(
                "$projectId|$pipelineId|${element.id}|${condition.targetField}|" +
                        "triggerOn:${eventValue}|${condition.operator}|$finalInputValue|$result"
            )
            if (!result) {
                return WebhookMatchResult(
                    isMatch = false,
                    reason = I18Variable(
                        WebhookI18nConstants.FIELD_CONDITION_NOT_MATCH,
                        params = listOf(condition.label, eventValue.toString())
                    ).toJsonStr()
                )
            }
        }
        return WebhookMatchResult(isMatch = true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventTriggerMatcher::class.java)
    }
}
