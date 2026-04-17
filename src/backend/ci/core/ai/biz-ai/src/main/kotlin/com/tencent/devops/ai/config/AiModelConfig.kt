package com.tencent.devops.ai.config

import com.tencent.devops.ai.properties.AiLlmProperties
import io.agentscope.core.formatter.openai.OpenAIMultiAgentFormatter
import io.agentscope.core.model.ExecutionConfig
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.ModelException
import io.agentscope.core.model.OpenAIChatModel
import io.agentscope.core.model.transport.HttpTransportConfig
import io.agentscope.core.model.transport.JdkHttpTransport
import org.slf4j.LoggerFactory
import java.time.Duration
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import java.util.function.Predicate

/** LLM 模型配置类，创建 OpenAIChatModel Bean。 */
@Configuration
@ConditionalOnWebApplication
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(AiLlmProperties::class)
class AiModelConfig {

    @Bean
    fun openAIChatModel(properties: AiLlmProperties): OpenAIChatModel {
        val useBkGateway = properties.bkAppCode.isNotBlank()
        logger.info(
            "[ModelConfig] Initializing OpenAIChatModel: " +
                "baseUrl={}, modelName={}, useBkGateway={}, " +
                "connectTimeout={}s, readTimeout={}s, writeTimeout={}s, " +
                "executionTimeout={}s, maxAttempts={}, initialBackoff={}s, " +
                "maxBackoff={}s, backoffMultiplier={}",
            properties.baseUrl, properties.modelName, useBkGateway,
            properties.connectTimeoutSeconds,
            properties.readTimeoutSeconds,
            properties.writeTimeoutSeconds,
            properties.executionTimeoutSeconds,
            properties.maxAttempts,
            properties.initialBackoffSeconds,
            properties.maxBackoffSeconds,
            properties.backoffMultiplier
        )

        val transportConfig = HttpTransportConfig.builder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds))
            .readTimeout(Duration.ofSeconds(properties.readTimeoutSeconds))
            .writeTimeout(Duration.ofSeconds(properties.writeTimeoutSeconds))
            .build()

        // 自定义重试谓词：框架 timeout() 产生的 ModelException 不携带 TimeoutException cause，
        // 导致内置 RETRYABLE_ERRORS 无法识别，需要额外匹配 ModelException 中的超时信息。
        val retryPredicate = Predicate<Throwable> { error ->
            if (error is ModelException && error.message?.contains("timeout", ignoreCase = true) == true) {
                return@Predicate true
            }
            ExecutionConfig.RETRYABLE_ERRORS.test(error)
        }

        val executionConfig = ExecutionConfig.builder()
            .timeout(Duration.ofSeconds(properties.executionTimeoutSeconds))
            .maxAttempts(properties.maxAttempts)
            .initialBackoff(Duration.ofSeconds(properties.initialBackoffSeconds))
            .maxBackoff(Duration.ofSeconds(properties.maxBackoffSeconds))
            .backoffMultiplier(properties.backoffMultiplier)
            .retryOn(retryPredicate)
            .build()

        val builder = OpenAIChatModel.builder()
            .baseUrl(properties.baseUrl)
            .modelName(properties.modelName)
            .stream(true)
            .httpTransport(JdkHttpTransport.builder().config(transportConfig).build())
            .formatter(OpenAIMultiAgentFormatter())

        if (useBkGateway) {
            // 蓝鲸 API 网关模式：通过 X-Bkapi-Authorization 认证，baseUrl 已包含完整端点路径
            val authJson = """{"bk_app_code":"${properties.bkAppCode}","bk_app_secret":"${properties.bkAppSecret}"}"""
            val options = GenerateOptions.builder()
                .additionalHeader("X-Bkapi-Authorization", authJson)
                .executionConfig(executionConfig)
                .build()
            builder.generateOptions(options).endpointPath("")
        } else {
            // 标准 OpenAI 兼容模式：通过 Bearer token 认证
            val options = GenerateOptions.builder()
                .executionConfig(executionConfig)
                .build()
            builder.apiKey(properties.apiKey).generateOptions(options)
        }

        return builder.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AiModelConfig::class.java
        )
    }
}
