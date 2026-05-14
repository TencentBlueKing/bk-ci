package com.tencent.devops.ai.agent.external

import com.tencent.devops.ai.agent.external.caller.ExternalAgentCaller
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import io.agentscope.core.message.ToolResultBlock
import io.agentscope.core.tool.AgentTool
import io.agentscope.core.tool.ToolCallParam
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * 外部智能体工具，将第三方平台的智能体包装为 agentscope-java 的
 * [AgentTool] 供 Supervisor 调用。
 *
 * 工具名和描述从数据库 [ExternalAgentInfo] 配置动态生成，
 * 实际调用通过 [ExternalAgentCaller] 策略接口路由到对应平台。
 */
class ExternalAgentTool(
    private val config: ExternalAgentInfo,
    private val caller: ExternalAgentCaller
) : AgentTool {

    override fun getName(): String {
        return config.agentName
            .lowercase()
            .replace(Regex("[^a-z0-9_\\u4e00-\\u9fff]"), "_")
            .replace(Regex("_+"), "_")
            .trimEnd('_')
    }

    override fun getDescription(): String {
        return "[${config.platform}] ${config.description}"
    }

    override fun getParameters(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "properties" to mapOf(
                "query" to mapOf(
                    "type" to "string",
                    "description" to "要发送给外部智能体的问题或指令"
                ),
                "conversation_id" to mapOf(
                    "type" to "string",
                    "description" to "会话ID，用于多轮对话时保持上下文。" +
                            "首次调用留空，后续可传入上次返回的会话ID"
                ),
                "chat_history" to mapOf(
                    "type" to "array",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "role" to mapOf("type" to "string"),
                            "content" to mapOf("type" to "string")
                        )
                    ),
                    "description" to "历史对话记录，用于为外部智能体提供上下文。" +
                            "格式: [{\"role\":\"user\",\"content\":\"...\"}, " +
                            "{\"role\":\"assistant\",\"content\":\"...\"}]"
                )
            ),
            "required" to listOf("query")
        )
    }

    override fun callAsync(
        param: ToolCallParam
    ): Mono<ToolResultBlock> {
        val query = param.input["query"]?.toString()
            ?: return Mono.just(
                ToolResultBlock.text("缺少 query 参数")
            )
        val conversationId = param.input["conversation_id"]?.toString() ?: ""
        val chatHistory = parseChatHistory(param.input["chat_history"])

        return Mono.fromCallable {
            logger.info(
                "[ExternalAgentTool] Calling: name={}, " +
                        "platform={}, conversationId={}, " +
                        "historySize={}, query={}",
                config.agentName, config.platform,
                conversationId.ifBlank { "(new)" },
                chatHistory.size,
                query.take(100)
            )
            val result = caller.call(
                config, query, conversationId, chatHistory
            )
            ToolResultBlock.text(result)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseChatHistory(
        raw: Any?
    ): List<Map<String, String>> {
        if (raw == null) return emptyList()
        return try {
            (raw as? List<*>)?.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val role = map["role"]?.toString() ?: return@mapNotNull null
                val content = map["content"]?.toString()
                    ?: return@mapNotNull null
                mapOf("role" to role, "content" to content)
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            ExternalAgentTool::class.java
        )
    }
}
