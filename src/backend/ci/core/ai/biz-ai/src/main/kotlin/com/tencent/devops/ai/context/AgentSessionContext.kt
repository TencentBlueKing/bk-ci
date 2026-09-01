package com.tencent.devops.ai.context

import com.tencent.devops.ai.pojo.ChatContextDTO
import io.agentscope.core.agent.Agent
import io.agentscope.core.agui.event.AguiEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

/**
 * 跨线程安全的 per-request 运行时上下文，整合所有 ConcurrentHashMap 数据。
 *
 * 请求结束时通过 [evictAll] 一次性清理指定 threadId 的所有关联数据，避免泄漏。
 *
 * 对于仅在请求线程可用的同步上下文（ChatContextDTO、threadId），
 * 由 [AiChatContext] 的 ThreadLocal 管理。
 */
@Component
class AgentSessionContext {

    /** threadId → userId */
    private val threadId2UserId = ConcurrentHashMap<String, String>()

    /** threadId → 最新聊天上下文（每次请求入口更新，供 Hook / Supplier lambda 跨线程读取） */
    private val threadChatContext = ConcurrentHashMap<String, ChatContextDTO>()

    /** agent 实例 → threadId */
    private val agentToThread = ConcurrentHashMap<Agent, String>()

    /** agent 实例 → 事件 Sink */
    private val agentSinks = ConcurrentHashMap<Agent, SinkInfo>()

    data class SinkInfo(
        val sink: Sinks.Many<AguiEvent>,
        val threadId: String,
        val runId: String
    )

    // ── threadId ↔ userId ──

    fun bindContext(
        threadId: String,
        userId: String,
        context: ChatContextDTO
    ) {
        threadId2UserId[threadId] = userId
        threadChatContext[threadId] = context
        logger.info(
            "[SessionCtx] bindContext: threadId={}, userId={}, mapSize={}",
            threadId, userId, threadId2UserId.size
        )
    }

    fun getUserId(threadId: String): String? = threadId2UserId[threadId]

    // ── threadId ↔ ChatContextDTO ──

    fun getChatContext(threadId: String): ChatContextDTO? = threadChatContext[threadId]

    // ── agent ↔ threadId ──

    fun registerAgentThread(agent: Agent, threadId: String) {
        agentToThread[agent] = threadId
    }

    fun getThreadIdByAgent(agent: Agent): String? = agentToThread[agent]

    // ── agent ↔ Sink ──

    fun registerSink(agent: Agent, info: SinkInfo) {
        agentSinks[agent] = info
    }

    fun getSinkByAgent(agent: Agent): SinkInfo? = agentSinks[agent]

    fun getSinkByThreadId(threadId: String): SinkInfo? =
        agentSinks.values.firstOrNull { it.threadId == threadId }

    fun removeSink(agent: Agent) {
        agentSinks.remove(agent)
    }

    // ── 统一清理 ──

    /**
     * 一次性清理指定 threadId 的所有关联数据。
     * 在请求结束（正常/异常/超时）时由 AiChatService.cleanup 调用。
     */
    fun evictAll(threadId: String) {
        threadId2UserId.remove(threadId)
        threadChatContext.remove(threadId)
        agentToThread.entries.removeIf { it.value == threadId }
        agentSinks.entries.removeIf { it.value.threadId == threadId }
        logger.debug(
            "[SessionCtx] evictAll: threadId={}, remaining: users={}, agents={}, sinks={}",
            threadId, threadId2UserId.size, agentToThread.size, agentSinks.size
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AgentSessionContext::class.java)
    }
}