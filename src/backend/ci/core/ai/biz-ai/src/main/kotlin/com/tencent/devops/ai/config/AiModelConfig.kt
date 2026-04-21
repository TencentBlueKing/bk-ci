package com.tencent.devops.ai.config

import com.tencent.devops.ai.properties.AiLlmProperties
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

        // 自定义重试谓词：在内置 RETRYABLE_ERRORS 基础上，补齐两类盲区：
        // 1) OpenAIException 家族（InternalServerException / 5xx / RateLimitException）——
        //    不继承 HttpTransportException，内置判定不认识，需显式识别。
        // 2) 框架 timeout() 及 SSE 流中断产生的裸 ModelException（message 匹配关键字）。
        // 同时显式短路 4xx 客户端错误（400/401/403/404/422）避免无意义重试。
        val retryPredicate = Predicate<Throwable> { error ->
            val retryable = evaluateRetryable(error)
            if (retryable) {
                logger.warn(
                    "[LLM-Retry] will retry: {} - {} | cause: {}",
                    error.javaClass.simpleName,
                    error.message,
                    describeCauseChain(error)
                )
            } else {
                logger.warn(
                    "[LLM-Retry] NOT retry: {} - {} | cause: {}",
                    error.javaClass.simpleName,
                    error.message,
                    describeCauseChain(error)
                )
            }
            retryable
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

    /**
     * 判定一个 Throwable 是否应触发重试。
     *
     * 规则优先级（从高到低）：
     * 1) 4xx 客户端错误 → 永不重试（参数/认证/权限/资源问题重试也是白费）
     * 2) 429 / 5xx / InternalServerException → 重试
     * 3) ModelException 且 message 命中流式可重试关键字 → 重试
     *    （覆盖 timeout、connection reset、broken pipe、premature close、unexpected end 等）
     * 4) 其余交由内置 RETRYABLE_ERRORS（HttpTransportException / TimeoutException / IOException
     *    及 cause 链递归）判定
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

    /**
     * 流式调用中常见的、值得重试的异常信息关键字。
     * 多数由 agentscope 框架在 Flux 层包装为裸 ModelException 抛出，
     * 不携带 IOException / TimeoutException 作为 cause，故需按 message 识别。
     */
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
        private val logger = LoggerFactory.getLogger(
            AiModelConfig::class.java
        )

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
