package com.tencent.devops.ai.config

import com.tencent.devops.ai.properties.AiMemoryProperties
import io.agentscope.core.memory.autocontext.AutoContextConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/** 智能体记忆配置类，创建 AutoContextConfig Bean。 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(AiMemoryProperties::class)
class AiMemoryConfig {

    @Bean
    fun autoContextConfig(
        properties: AiMemoryProperties
    ): AutoContextConfig {
        logger.info(
            "[MemoryConfig] AutoContextConfig: " +
                "msgThreshold={}, lastKeep={}, " +
                "tokenRatio={}, maxToken={}, " +
                "offloadSinglePreview={}, " +
                "currentRoundCompressionRatio={}, " +
                "largePayloadThreshold={}, " +
                "minConsecutiveToolMessages={}, " +
                "minCompressionTokenThreshold={}",
            properties.msgThreshold, properties.lastKeep,
            properties.tokenRatio, properties.maxToken,
            properties.offloadSinglePreview,
            properties.currentRoundCompressionRatio,
            properties.largePayloadThreshold,
            properties.minConsecutiveToolMessages,
            properties.minCompressionTokenThreshold
        )
        return AutoContextConfig.builder()
            .msgThreshold(properties.msgThreshold)
            .lastKeep(properties.lastKeep)
            .tokenRatio(properties.tokenRatio)
            .maxToken(properties.maxToken)
            .offloadSinglePreview(properties.offloadSinglePreview)
            .currentRoundCompressionRatio(properties.currentRoundCompressionRatio)
            .largePayloadThreshold(properties.largePayloadThreshold)
            .minConsecutiveToolMessages(properties.minConsecutiveToolMessages)
            .minCompressionTokenThreshold(properties.minCompressionTokenThreshold)
            .build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiMemoryConfig::class.java
        )
    }
}
