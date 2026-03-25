package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.pojo.element.market.MarketEventAtomElement
import com.tencent.devops.common.webhook.service.code.pojo.WebhookMatchResult
import com.tencent.devops.process.pojo.trigger.GenericWebhookEventBody
import com.tencent.devops.process.constant.ProcessMessageCode.BK_TRIGGER_EVENT_CONFIG_NOT_FOUND_DESC
import com.tencent.devops.process.pojo.trigger.TriggerEventBody
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FIELD_CONDITION_EXCLUDE
import com.tencent.devops.process.constant.ProcessMessageCode.BK_FIELD_CONDITION_NOT_MATCH
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse
import com.tencent.devops.process.utils.PIPELINE_BUILD_MSG
import com.tencent.devops.store.api.common.ServiceStoreComponentResource
import com.tencent.devops.store.pojo.common.KEY_INPUT
import com.tencent.devops.store.pojo.common.KEY_TRIGGER_EVENT_CONFIG
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.trigger.TriggerEventConfig
import com.tencent.devops.store.pojo.trigger.conditions.InputCondition
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperatorEnum
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
        triggerEventBody: TriggerEventBody,
        variables: Map<String, String>,
        element: MarketEventAtomElement,
        extStartParam: Map<String, String>
    ): WebhookAtomResponse {
        // 根据插件版本,获取事件字段映射和触发条件
        val atomCode = element.atomCode
        val version = element.version
        logger.info("start to match event trigger|$projectId|$pipelineId|$atomCode@$version|${element.id}")
        val componentDetail = client.get(ServiceStoreComponentResource::class).getComponentDataInfoByCode(
            storeType = StoreTypeEnum.TRIGGER_EVENT,
            storeCode = atomCode,
            version = version,
            status = StoreStatusEnum.RELEASED
        ).data ?: return WebhookAtomResponse(
            matchStatus = MatchStatus.ELEMENT_NOT_MATCH,
            failedReason = I18Variable(
                BK_TRIGGER_EVENT_CONFIG_NOT_FOUND_DESC,
                params = listOf()
            ).toJsonStr()
        )
        // 事件配置
        val triggerConditionMap = componentDetail.extData?.get(KEY_TRIGGER_EVENT_CONFIG) as Map<String, Any>?
        if (triggerConditionMap.isNullOrEmpty()) {
            throw InvalidParamException("trigger condition not found")
        }
        val eventConfig = JsonUtil.mapTo(triggerConditionMap, TriggerEventConfig::class.java)
        triggerEventBody as GenericWebhookEventBody
        // 解析request获取映射字段的值
        val eventVariables = marketEventVariablesResolver.getEventVariables(
            fieldMappings = eventConfig.fieldMapping,
            incomingHeaders = triggerEventBody.headers,
            incomingQueryParamMap = triggerEventBody.queryParams,
            incomingBody = triggerEventBody.body
        )
        // val fieldMap = eventConfig.fieldMapping.associate { it.sourcePath to it.targetField }
        // 计算匹配结果
        val matchResult = evaluate(
            projectId = projectId,
            pipelineId = pipelineId,
            element = element,
            variables = variables,
            eventConfig = eventConfig,
            eventVariables = eventVariables,
            extStartParam = extStartParam
        )
        return if (matchResult.isMatch) {
            // 生成启动参数
            val startParams = mutableMapOf<String, Any>()
            startParams.putAll(variables)
            startParams.putAll(eventVariables)
            startParams[PIPELINE_BUILD_MSG] = componentDetail.name
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
        extStartParam: Map<String, String>
    ): WebhookMatchResult {
        val conditionLabels = eventConfig.conditions.groupBy { it.label }
        eventConfig.conditions.forEach { condition ->
            // 触发变量值
            val eventValue = eventVariables[condition.targetField] ?: extStartParam[condition.targetField] ?: ""
            val input = element.data[KEY_INPUT] as Map<String, Any>? ?: mapOf()
            // 目标变量值
            val inputValue = input[condition.key()]?.let {
                when (it) {
                    is String -> EnvUtils.parseEnv(it, variables).takeIf { it.isNotBlank() }
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
                    inputValue?.toString()?.takeIf { it.isNotBlank() }?.split(condition.separator!!)

                else -> inputValue
            }
            val expression = condition.operator.expression
            val result = expression.evaluate(eventValue, finalInputValue)
            logger.info(
                "$projectId|$pipelineId|${element.id}|${condition.targetField}|" +
                        "triggerOn:$eventValue|${condition.operator}|$finalInputValue|$result"
            )
            if (!result) {
                val messageCode = when (condition.operator) {
                    ConditionOperatorEnum.EQ, ConditionOperatorEnum.IN, ConditionOperatorEnum.LIKE ->
                        BK_FIELD_CONDITION_NOT_MATCH

                    ConditionOperatorEnum.NOT_LIKE, ConditionOperatorEnum.NOT_EQ, ConditionOperatorEnum.NOT_IN ->
                        BK_FIELD_CONDITION_EXCLUDE
                }
                // 当存在多个同名label时，需添加组名用于区分
                val label = if (conditionLabels.getOrDefault(condition.label, listOf()).size > 1) {
                    listOf(condition.group, condition.label).filter { !it.isNullOrBlank() }.joinToString("-")
                } else {
                    condition.label
                }
                return WebhookMatchResult(
                    isMatch = false,
                    reason = I18Variable(
                        messageCode,
                        params = listOf(
                            label,
                            eventValue.toString(),
                            if (finalInputValue is List<*>) {
                                finalInputValue.joinToString(", ")
                            } else {
                                finalInputValue.toString()
                            }
                        )
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
