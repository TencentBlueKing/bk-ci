package com.tencent.devops.ai.model

import io.agentscope.core.model.ExecutionConfig
import io.agentscope.core.model.ModelException
import io.agentscope.core.model.exception.AuthenticationException
import io.agentscope.core.model.exception.BadRequestException
import io.agentscope.core.model.exception.InternalServerException
import io.agentscope.core.model.exception.NotFoundException
import io.agentscope.core.model.exception.OpenAIException
import io.agentscope.core.model.exception.PermissionDeniedException
import io.agentscope.core.model.exception.RateLimitException
import io.agentscope.core.model.exception.UnprocessableEntityException
import org.springframework.stereotype.Component

/**
 * LLM 调用错误分类器。
 *
 * 同时被「模型内部重试 (AiModelFactory)」和「failover 链切换 (FailoverChatModel)」复用，
 * 确保两层对错误的判定语义完全一致：
 *
 * - 4xx 客户端错误（400/401/403/404/422）→ 永久错误，不重试也不切换
 * - 429 / 5xx / 流式中断 / IO / 超时 → 临时错误，可重试可切换
 * - 未识别的异常 → 走 [ExecutionConfig.RETRYABLE_ERRORS] 兜底判定
 */
@Component
class AiErrorClassifier {

    /**
     * 判定一个 Throwable 是否应触发重试 / failover。
     *
     * 优先级（从高到低）：
     * 1. 4xx 客户端错误 → false（参数/认证/权限/资源问题重试也是白费）
     * 2. 429 / 5xx / [InternalServerException] / [OpenAIException] 5xx → true
     * 3. [ModelException] 且 message 命中流式可重试关键字 → true
     * 4. 其余交由 [ExecutionConfig.RETRYABLE_ERRORS]（识别 IOException / TimeoutException
     *    / HttpTransportException 及 cause 链）判定
     */
    fun isRetryable(error: Throwable): Boolean {
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
     * 描述异常的 cause 链（最多 5 层），用于日志输出。
     *
     * 当 cause 链为空时返回 "none"，否则返回 "ClassA(msgA) -> ClassB(msgB) -> ..."。
     */
    fun describeCauseChain(error: Throwable): String {
        val sb = StringBuilder()
        var current: Throwable? = error.cause
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
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

    private fun isStreamingRetryable(message: String?): Boolean {
        if (message.isNullOrBlank()) return false
        return RETRYABLE_MESSAGE_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    companion object {
        private const val MAX_CAUSE_DEPTH = 5

        /**
         * 流式调用中常见的、值得重试的异常信息关键字。
         * 多数由 agentscope 框架在 Flux 层包装为裸 [ModelException] 抛出，
         * 不携带 IOException / TimeoutException 作为 cause，故需按 message 识别。
         */
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
