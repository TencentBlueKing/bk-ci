package com.tencent.devops.ai.util

import reactor.core.Exceptions

/**
 * 将模型 / Reactor 抛出的异常统一翻译为用户可读的中文提示。
 *
 * 同时兼容：
 * 1. Reactor 直接抛出的 retry exhausted 异常
 * 2. retry exhausted 被上层再次包装后的异常链
 */
object AiErrorMessageTranslator {

    fun toFriendlyMessage(error: Throwable): String {
        val errors = generateSequence(error) { it.cause }
            .take(MAX_CAUSE_DEPTH)
            .toList()
        val messages = errors.mapNotNull { it.message?.takeIf(String::isNotBlank) }

        val hasRetryExhausted = errors.any { current ->
            Exceptions.isRetryExhausted(current) || current.message.orEmpty()
                .contains("Retries exhausted", ignoreCase = true)
        }
        val hasTimeout = messages.any(::containsTimeoutKeyword)
        val hasNetwork = messages.any(::containsNetworkKeyword)

        return when {
            hasRetryExhausted && hasTimeout -> MSG_RETRY_TIMEOUT
            hasRetryExhausted && hasNetwork -> MSG_RETRY_NETWORK
            hasRetryExhausted -> MSG_RETRY_EXHAUSTED
            hasTimeout -> MSG_TIMEOUT
            hasNetwork -> MSG_NETWORK
            else -> MSG_UNKNOWN
        }
    }

    private fun containsTimeoutKeyword(message: String): Boolean {
        return TIMEOUT_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    private fun containsNetworkKeyword(message: String): Boolean {
        return NETWORK_KEYWORDS.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    private const val MAX_CAUSE_DEPTH = 8

    private val TIMEOUT_KEYWORDS = listOf(
        "timeout",
        "timed out"
    )

    private val NETWORK_KEYWORDS = listOf(
        "stream failed",
        "connection",
        "connection reset",
        "broken pipe",
        "stream closed",
        "premature",
        "unexpected end"
    )

    private const val MSG_RETRY_TIMEOUT =
        "AI 模型响应超时，已重试多次仍未成功，请稍后再试。"
    private const val MSG_RETRY_NETWORK =
        "与 AI 模型的网络连接异常，已重试多次仍未恢复，请稍后再试。"
    private const val MSG_RETRY_EXHAUSTED =
        "AI 模型服务暂时不可用，已重试多次仍未成功，请稍后再试。"
    private const val MSG_TIMEOUT =
        "AI 模型响应超时，请稍后再试。"
    private const val MSG_NETWORK =
        "与 AI 模型的网络连接异常，请稍后再试。"
    private const val MSG_UNKNOWN =
        "AI 服务出现异常，请稍后再试。"
}
