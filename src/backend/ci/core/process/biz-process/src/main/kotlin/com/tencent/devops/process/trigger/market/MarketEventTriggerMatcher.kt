package com.tencent.devops.process.trigger.market

import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.webhook.pojo.WebhookRequest
import com.tencent.devops.process.trigger.enums.MatchStatus
import com.tencent.devops.process.trigger.pojo.WebhookAtomResponse

class MarketEventTriggerMatcher {

    fun matches(
        projectId: String,
        pipelineId: String,
        webhookRequest: WebhookRequest,
        variables: Map<String, String>,
        element: MarketBuildLessAtomElement
    ): WebhookAtomResponse {
        // 1. 根据插件版本,获取事件字段映射和触发条件


        // 2. 解析body获取映射字段的值


        // 3. 获取插件配置的值
        // 4. 映射的值与插件配置的值进行匹配
        val startParams = mutableMapOf<String, Any>()
        return WebhookAtomResponse(
            matchStatus = MatchStatus.SUCCESS,
            outputVars = startParams
        )
    }
}
