package com.tencent.devops.gpt.service.config

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class GptGatewayCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val environment = context.environment
        val gptGateway = environment.getProperty("gpt.gateway")
        return !gptGateway.isNullOrBlank()
    }
}
