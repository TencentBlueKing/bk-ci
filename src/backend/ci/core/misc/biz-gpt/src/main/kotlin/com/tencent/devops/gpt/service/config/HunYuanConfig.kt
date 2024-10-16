package com.tencent.devops.gpt.service.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Component

@Component
@Conditional(GptGatewayCondition::class)
class HunYuanConfig {
    @Value("\${gpt.gateway:}")
    val url = ""

    @Value("#{\${gpt.headers:{}}}")
    val headers: Map<String, String> = emptyMap()
}
