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
 * Knot 平台智能体调用器，通过 AG-UI 协议调用外部 Knot 智能体。
 * 作为 Tool 使用时采用非流式模式。
 */
@Component
class KnotAgentCaller : ExternalAgentCaller {

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
        val body = mapOf(
            "input" to mapOf(
                "message" to query,
                "conversation_id" to conversationId,
                "stream" to false
            )
        )

        logger.info(
            "[KnotCaller] Calling: name={}, url={}",
            config.agentName, config.apiUrl
        )

        val response = webClient.post()
            .uri(config.apiUrl)
            .headers { h -> headers.forEach { (k, v) -> h.set(k, v) } }
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .block() ?: return "外部智能体无响应"

        return extractContent(response)
    }

    private fun extractContent(response: String): String {
        return try {
            val lines = response.lines()
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .filter { it != "[DONE]" && it.isNotBlank() }

            if (lines.isEmpty()) {
                val json: Map<String, Any> = objectMapper.readValue(response)
                @Suppress("UNCHECKED_CAST")
                val input = json["input"] as? Map<String, Any>
                return input?.get("message")?.toString() ?: response
            }

            val result = StringBuilder()
            for (line in lines) {
                try {
                    val event: Map<String, Any> = objectMapper.readValue(line)
                    val type = event["type"]?.toString() ?: continue
                    if (type == "TEXT_MESSAGE_CONTENT") {
                        val rawEvent = event["rawEvent"] as? Map<*, *>
                        val content = rawEvent?.get("content")?.toString()
                        if (content != null) result.append(content)
                    }
                } catch (_: Exception) {
                    // skip unparseable lines
                }
            }
            result.toString().ifBlank { response }
        } catch (e: Exception) {
            logger.warn("[KnotCaller] Parse error: {}", e.message)
            response
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            KnotAgentCaller::class.java
        )
        private val objectMapper = jacksonObjectMapper()
        const val PLATFORM = "KNOT"
        private const val TIMEOUT_SECONDS = 60L
        private const val MAX_RESPONSE_SIZE = 10 * 1024 * 1024
    }
}
