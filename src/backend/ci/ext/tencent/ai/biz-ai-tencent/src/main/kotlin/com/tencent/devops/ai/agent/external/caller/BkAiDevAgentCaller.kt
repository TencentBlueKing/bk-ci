package com.tencent.devops.ai.agent.external.caller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.service.AiMcpServerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * Calls BK AI Dev agents via SSE protocol.
 * Parses event types: text, think, done, error.
 */
@Component
class BkAiDevAgentCaller : ExternalAgentCaller {

    private val webClient = WebClient.builder()
        .codecs { it.defaultCodecs().maxInMemorySize(MAX_RESPONSE_SIZE) }
        .build()

    override fun platform(): String = PLATFORM

    override fun call(
        config: ExternalAgentInfo,
        query: String,
        conversationId: String,
        chatHistory: List<Map<String, String>>
    ): String {
        val headers = AiMcpServerService.parseHeaders(config.headers)
        val executeKwargs = mutableMapOf<String, Any>("stream" to true)
        if (conversationId.isNotBlank()) {
            executeKwargs["thread_id"] = conversationId
        }
        val body = mapOf(
            "input" to query,
            "chat_history" to chatHistory.ifEmpty { emptyList<Any>() },
            "execute_kwargs" to executeKwargs
        )

        logger.info(
            "[BkAiDevCaller] Calling: name={}, url={}",
            config.agentName, config.apiUrl
        )

        val response = webClient.post()
            .uri(config.apiUrl)
            .header("Content-Type", "application/json")
            .headers { h -> headers.forEach { (k, v) -> h.set(k, v) } }
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .block() ?: return "外部智能体无响应"

        return extractContent(response)
    }

    private fun extractContent(response: String): String {
        val result = StringBuilder()
        val lines = response.lines()
            .map { it.removePrefix("data:").trim() }
            .filter { it.isNotBlank() }

        for (line in lines) {
            try {
                val event: Map<String, Any> = objectMapper.readValue(line)
                when (event["event"]?.toString()) {
                    "text" -> {
                        val content = event["content"]?.toString()
                        if (content != null) result.append(content)
                    }
                    "done" -> {
                        val cover = event["cover"] as? Boolean ?: false
                        if (cover) {
                            val content = event["content"]?.toString()
                            if (content != null) return content
                        }
                        break
                    }
                    "error" -> {
                        val message = event["message"]?.toString()
                            ?: "未知错误"
                        logger.warn(
                            "[BkAiDevCaller] Error from agent: {}",
                            message
                        )
                        return "外部智能体返回错误: $message"
                    }
                }
            } catch (_: Exception) {
                // skip unparseable lines
            }
        }
        return result.toString().ifBlank { response }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            BkAiDevAgentCaller::class.java
        )
        private val objectMapper = jacksonObjectMapper()
        const val PLATFORM = "BKAIDEV"
        private const val TIMEOUT_SECONDS = 60L
        private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024
    }
}
