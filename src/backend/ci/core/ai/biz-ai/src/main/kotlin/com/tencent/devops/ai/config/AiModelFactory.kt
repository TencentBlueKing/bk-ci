package com.tencent.devops.ai.config

import com.tencent.devops.ai.properties.AiLlmModelProperties
import io.agentscope.core.formatter.openai.OpenAIMultiAgentFormatter
import io.agentscope.core.model.ExecutionConfig
import io.agentscope.core.model.GenerateOptions
import io.agentscope.core.model.ModelException
import io.agentscope.core.model.OpenAIChatModel
import io.agentscope.core.model.exception.AuthenticationException
import io.agentscope.core.model.exception.BadRequestException
import io.agentscope.core.model.exception.InternalServerException
import io.agentscope.core.model.exception.NotFoundException
import io.agentscope.core.model.exception.OpenAIException
import io.agentscope.core.model.exception.PermissionDeniedException
import io.agentscope.core.model.exception.RateLimitException
import io.agentscope.core.model.exception.UnprocessableEntityException
import io.agentscope.core.model.transport.HttpTransportConfig
import io.agentscope.core.model.transport.JdkHttpTransport
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.function.Predicate

@Component
class AiModelFactory {

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
        logger.info(
            "[ModelFactory] Initializing OpenAIChatModel: id={}, baseUrl={}, modelName={}, useBkGateway={}, " +
                "connectTimeout={}s, readTimeout={}s, writeTimeout={}s, executionTimeout={}s, " +
                "maxAttempts={}, configuredMaxAttempts={}, initialBackoff={}s, maxBackoff={}s, " +
                "backoffMultiplier={}, retryMode={}",
            properties.id,
            properties.baseUrl,
            properties.modelName,
            useBkGateway,
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
            val retryable = evaluateRetryable(error)
            if (retryable && maxAttempts > SINGLE_ATTEMPT) {
                logger.warn(
                    "[LLM-Retry] will retry modelId={}, retryMode={}, maxAttempts={}: {} - {} | cause: {}",
                    properties.id,
                    retryMode,
                    maxAttempts,
                    error.javaClass.simpleName,
                    error.message,
                    describeCauseChain(error)
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
                    describeCauseChain(error)
                )
            } else {
                logger.warn(
                    "[LLM-Retry] NOT retry modelId={}, retryMode={}, maxAttempts={}: {} - {} | cause: {}",
                    properties.id,
                    retryMode,
                    maxAttempts,
                    error.javaClass.simpleName,
                    error.message,
                    describeCauseChain(error)
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
            val options = GenerateOptions.builder()
                .additionalHeader("X-Bkapi-Authorization", authJson)
                .executionConfig(executionConfig)
                .build()
            builder.generateOptions(options).endpointPath("")
        } else {
            val options = GenerateOptions.builder()
                .executionConfig(executionConfig)
                .build()
            builder.apiKey(properties.apiKey).generateOptions(options)
        }
        return builder.build()
    }

    /**
     * 判定一个 Throwable 是否应触发重试。
     */
    private fun evaluateRetryable(error: Throwable): Boolean {
        return when {
            error is BadRequestException -> false
            error is AuthenticationException -> false
            error is PermissionDeniedException -> false
            error is NotFoundException -> false
            error is UnprocessableEntityException -> false

            error is RateLimitException -> true
            error is InternalServerException -> true
            error is OpenAIException && (error.statusCode ?: 0) >= 500 -> true

            error is ModelException && isStreamingRetryable(error.message) -> true

            else -> ExecutionConfig.RETRYABLE_ERRORS.test(error)
        }
    }

    private fun isStreamingRetryable(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return RETRYABLE_MESSAGE_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    private fun describeCauseChain(error: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = error.cause
        var depth = 0
        while (current != null && depth < 5) {
            if (sb.isNotEmpty()) sb.append(" -> ")
            sb.append(current.javaClass.simpleName)
                .append("(")
                .append(current.message ?: "null")
                .append(")")
            current = current.cause
            depth++
        }
        return sb.ifEmpty { "none" }.toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiModelFactory::class.java)

        private const val SINGLE_ATTEMPT = 1

        private val RETRYABLE_MESSAGE_KEYWORDS = listOf(
            "timeout",
            "timed out",
            "connection reset",
            "broken pipe",
            "connection closed",
            "stream closed",
            "premature",
            "unexpected end"
        )
    }
}
