package com.tencent.devops.ai.context

import com.tencent.devops.ai.pojo.ChatContextDTO

/**
 * 当前 AG-UI 对话请求的 ThreadLocal 上下文。
 *
 * 仅存储请求线程内可靠的同步上下文（ChatContextDTO、threadId），
 * 不持有任何 ConcurrentHashMap。
 *
 * 跨线程安全的运行时数据（agent 映射、Sink、持久化暂存等）
 * 统一由 [AgentSessionContext] 管理。
 */
object AiChatContext {
    private val contextHolder = ThreadLocal<ChatContextDTO>()
    private val threadIdHolder = ThreadLocal<String>()

    fun setContext(context: ChatContextDTO) {
        contextHolder.set(context)
    }

    fun getContext(): ChatContextDTO =
        contextHolder.get() ?: ChatContextDTO()

    fun setThreadId(threadId: String) {
        threadIdHolder.set(threadId)
    }

    fun getThreadId(): String? = threadIdHolder.get()

    fun clear() {
        contextHolder.remove()
        threadIdHolder.remove()
    }
}