package com.tencent.devops.ai.config

import com.tencent.devops.ai.properties.AiLlmProperties
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/** LLM 模型配置类，启用模型配置属性绑定。 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(AiLlmProperties::class)
class AiModelConfig
