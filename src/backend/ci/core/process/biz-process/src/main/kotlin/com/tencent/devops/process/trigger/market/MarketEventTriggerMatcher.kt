package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_CONFIG
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerEventConfig
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperator
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
        element: MarketBuildLessAtomElement
    ): WebhookAtomResponse {
        // 1. 根据插件版本,获取事件字段映射和触发条件
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
        // 2. 解析request获取映射字段的值
        val triggerParams = marketEventVariablesResolver.getEventVariables(
            fieldMappings = eventConfig.fieldMapping,
            incomingHeaders = webhookRequest.headers,
            incomingQueryParamMap = webhookRequest.queryParams,
            incomingBody = webhookRequest.body
        )
        val fieldMap = eventConfig.fieldMapping.associate { it.sourcePath to it.targetField }
        // 3. 获取插件配置的值
        eventConfig.conditions.forEach { condition ->
            val envValue = fieldMap[condition.refField]
            // 触发变量值
            val triggerValue = triggerParams[envValue] ?: ""
            val input = element.data[KEY_INPUT] as Map<String, Any>? ?: mapOf()
            // 目标变量值
            val targetValue = input[condition.key()]?.let {
                when (it) {
                    is String -> EnvUtils.parseEnv(it, variables)
                    is List<*> -> it.map { item ->
                        item as String
                        EnvUtils.parseEnv(item, variables)
                    }
                    else -> InvalidParamException("unsupported type of $it")
                }
            }
            val match = match(
                operator = condition.operator,
                targetValue = targetValue,
                triggerValue = triggerValue
            )
            logger.info(
                "$projectId|$pipelineId|${element.id}|${condition.refField}|" +
                        "triggerOn:${triggerValue}|${condition.operator}|${targetValue}|$match"
            )
            if (!match) {
                return WebhookAtomResponse(
                    matchStatus = MatchStatus.CONDITION_NOT_MATCH,
                    outputVars = mapOf(),
                    failedReason = I18Variable(
                        WebhookI18nConstants.FIELD_CONDITION_NOT_MATCH,
                        params = listOf(condition.label, triggerValue)
                    ).toJsonStr()
                )
            }
        }
        // 4. 映射的值与插件配置的值进行匹配
        val startParams = mutableMapOf<String, Any>()
        return WebhookAtomResponse(
            matchStatus = MatchStatus.SUCCESS,
            outputVars = startParams
        )
    }

    fun match(
        operator: ConditionOperator,
        triggerValue: String,
        targetValue: Any?
    ): Boolean {
        return when (operator) {
            ConditionOperator.EQ -> triggerValue == targetValue.toString()
            ConditionOperator.NOT_EQ -> triggerValue != targetValue.toString()
            ConditionOperator.IN -> {
                when (targetValue) {
                    is List<*> -> targetValue.contains(triggerValue)
                    is String -> targetValue.split(",").contains(triggerValue)
                    else -> false
                }
            }

            ConditionOperator.NOT_IN -> {
                when (targetValue) {
                    is List<*> -> !targetValue.contains(triggerValue)
                    is String -> !targetValue.split(",").contains(triggerValue)
                    else -> false
                }
            }

            ConditionOperator.LIKE -> {
                when (targetValue) {
                    is List<*> -> targetValue.any {
                        doLike(it, triggerValue)
                    }
                    is String -> targetValue.split(",").any{
                        doLike(it, triggerValue)
                    }

                    else -> false
                }
            }

            ConditionOperator.NOT_LIKE -> {
                when (targetValue) {
                    is List<*> -> targetValue.none {
                        doLike(it, triggerValue)
                    }
                    is String -> !targetValue.split(",").any{
                        doLike(it, triggerValue)
                    }

                    else -> false
                }
            }
        }
    }

    private fun doLike(targetValue: Any?, triggerValue: String): Boolean {
        val pattern = (targetValue as String? ?: "").replace("%", ".*")
        return Regex(pattern).matches(triggerValue)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MarketEventTriggerMatcher::class.java)
    }
}