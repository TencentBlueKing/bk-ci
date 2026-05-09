package com.tencent.devops.ai.util

import io.agentscope.core.agui.encoder.AguiEventEncoder
import io.agentscope.core.agui.event.AguiEvent
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import reactor.core.publisher.Sinks

/**
 * SSE 事件推送工具，统一处理向前端写入错误/超时终止事件的逻辑。
 *
 * 支持两种推送方式：
 * - [writeErrorAndFinish]：直接写入 [ChunkedOutput]（用于 Resource / Service 层）
 * - [emitErrorAndFinish]：通过 Reactor [Sinks.Many] 发射（用于 Hook 等无法直接访问 output 的场景）
 */
object SseEventWriter {

    private val logger = LoggerFactory.getLogger(SseEventWriter::class.java)

    private val encoder = AguiEventEncoder()

    /**
     * 向 SSE 输出写入 Raw(error) + RunFinished，确保前端能感知错误并终止流。
     * 写入失败时静默忽略（客户端可能已断开）。
     */
    fun writeErrorAndFinish(
        output: ChunkedOutput<String>,
        threadId: String?,
        runId: String?,
        errorMessage: String
    ) {
        try {
            val raw = AguiEvent.Raw(threadId, runId, mapOf("error" to errorMessage))
            val finish = AguiEvent.RunFinished(threadId, runId)
            output.write(encoder.encode(raw))
            output.write(encoder.encode(finish))
        } catch (e: Exception) {
            logger.debug("[SseEventWriter] Failed to write error events, client may have disconnected: {}", e.message)
        }
    }

    /**
     * 通过 Reactor Sink 发射 Raw(error) + RunFinished 事件。
     * 事件会经由 mergedEvents 流到达 subscribeAndAwait 的 onNext，最终写入 SSE 输出。
     */
    fun emitErrorAndFinish(
        sink: Sinks.Many<AguiEvent>,
        threadId: String?,
        runId: String?,
        errorMessage: String
    ) {
        try {
            val raw = AguiEvent.Raw(threadId, runId, mapOf("error" to errorMessage))
            val finish = AguiEvent.RunFinished(threadId, runId)
            sink.tryEmitNext(raw)
            sink.tryEmitNext(finish)
        } catch (e: Exception) {
            logger.debug("[SseEventWriter] Failed to emit error events to sink: {}", e.message)
        }
    }
}
