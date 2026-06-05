package com.tencent.devops.ai.config

import com.tencent.devops.ai.model.AiErrorClassifier
import com.tencent.devops.ai.properties.AiLlmModelProperties
import io.agentscope.core.formatter.openai.OpenAIMultiAgentFormatter
import io.agentscope.core.model.ExecutionConfig
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.OpenAIChatModel
import io.agentscope.core.model.transport.HttpTransportConfig
import io.agentscope.core.model.transport.JdkHttpTransport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Predicate

@Component
class AiModelFactory(
    private val errorClassifier: AiErrorClassifier
) {

    fun create(properties: AiLlmModelProperties): OpenAIChatModel {
        return create(
            properties = properties,
            maxAttempts = properties.maxAttempts,
            retryMode = "configured"
        )
    }

    fun createSingleAttempt(properties: AiLlmModelProperties): OpenAIChatModel {
        return create(
            properties = properties,
            maxAttempts = SINGLE_ATTEMPT,
            retryMode = "single-attempt-failover"
        )
    }

    private fun create(
        properties: AiLlmModelProperties,
        maxAttempts: Int,
        retryMode: String
    ): OpenAIChatModel {
        val useBkGateway = properties.useBkGateway()
        val effectiveReasoningEffort = properties.reasoningEffort?.takeIf { it.isNotBlank() }
        logger.info(
            "[ModelFactory] Initializing OpenAIChatModel: id={}, baseUrl={}, modelName={}, useBkGateway={}, " +
                "reasoningEffort={}, " +
                "connectTimeout={}s, readTimeout={}s, writeTimeout={}s, executionTimeout={}s, " +
                "maxAttempts={}, configuredMaxAttempts={}, initialBackoff={}s, maxBackoff={}s, " +
                "backoffMultiplier={}, retryMode={}",
            properties.id,
            properties.baseUrl,
            properties.modelName,
            useBkGateway,
            effectiveReasoningEffort ?: "<unset>",
            properties.connectTimeoutSeconds,
            properties.readTimeoutSeconds,
            properties.writeTimeoutSeconds,
            properties.executionTimeoutSeconds,
            maxAttempts,
            properties.maxAttempts,
            properties.initialBackoffSeconds,
            properties.maxBackoffSeconds,
            properties.backoffMultiplier,
            retryMode
        )

        val transportConfig = HttpTransportConfig.builder()
            .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds))
            .readTimeout(Duration.ofSeconds(properties.readTimeoutSeconds))
            .writeTimeout(Duration.ofSeconds(properties.writeTimeoutSeconds))
            .build()

        val retryPredicate = Predicate<Throwable> { error ->
            val retryable = errorClassifier.isRetryable(error)
            val cause = errorClassifier.describeCauseChain(error)
            if (retryable && maxAttempts > SINGLE_ATTEMPT) {
                logger.warn(
                    "[LLM-Retry] will retry modelId={}, retryMode={}, maxAttempts={}: {} - {} | cause: {}",
                    properties.id,
                    retryMode,
                    maxAttempts,
                    error.javaClass.simpleName,
                    error.message,
                    cause
                )
            } else if (retryable) {
                logger.warn(
                    "[LLM-Retry] retryable but same-model retry disabled, " +
                        "modelId={}, retryMode={}, maxAttempts={}: {} - {} | cause: {}",
                    properties.id,
                    retryMode,
                    maxAttempts,
                    error.javaClass.simpleName,
                    error.message,
                    cause
                )
            } else {
                logger.warn(
                    "[LLM-Retry] NOT retry modelId={}, retryMode={}, maxAttempts={}: {} - {} | cause: {}",
                    properties.id,
                    retryMode,
                    maxAttempts,
                    error.javaClass.simpleName,
                    error.message,
                    cause
                )
            }
            retryable
        }

        val executionConfig = ExecutionConfig.builder()
            .timeout(Duration.ofSeconds(properties.executionTimeoutSeconds))
            .maxAttempts(maxAttempts)
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
            val authJson =
                """{"bk_app_code":"${properties.bkAppCode}","bk_app_secret":"${properties.bkAppSecret}"}"""
            val optionsBuilder = GenerateOptions.builder()
                .additionalHeader("X-Bkapi-Authorization", authJson)
                .executionConfig(executionConfig)
            effectiveReasoningEffort?.let { optionsBuilder.reasoningEffort(it) }
            builder.generateOptions(optionsBuilder.build()).endpointPath("")
        } else {
            val optionsBuilder = GenerateOptions.builder()
                .executionConfig(executionConfig)
            effectiveReasoningEffort?.let { optionsBuilder.reasoningEffort(it) }
            builder.apiKey(properties.apiKey).generateOptions(optionsBuilder.build())
        }
        return builder.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiModelFactory::class.java)

        private const val SINGLE_ATTEMPT = 1
    }
}
