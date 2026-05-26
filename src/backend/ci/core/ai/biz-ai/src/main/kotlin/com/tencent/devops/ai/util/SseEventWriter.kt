package com.tencent.devops.ai.util

import io.agentscope.core.agui.encoder.AguiEventEncoder
import io.agentscope.core.agui.event.AguiEvent
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import reactor.core.publisher.Sinks
import java.time.Duration

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
     * 多线程并发 emit 时，[Sinks.Many.tryEmitNext] 可能返回 `FAIL_NON_SERIALIZED` 被静默丢弃。
     * 用 busyLooping 在短窗口内自旋重试，确保关键终止事件不丢。
     */
    private val emitFailureHandler = Sinks.EmitFailureHandler.busyLooping(
        Duration.ofMillis(EMIT_BUSY_LOOP_MILLIS)
    )

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
        val raw = AguiEvent.Raw(threadId, runId, mapOf("error" to errorMessage))
        val finish = AguiEvent.RunFinished(threadId, runId)
        emitOrLog(sink, raw, "Raw(error)", threadId, runId)
        emitOrLog(sink, finish, "RunFinished", threadId, runId)
    }

    /**
     * 通过 Reactor Sink 仅发射 RunFinished 事件（无错误信息）。
     * 用于用户主动停止等正常终止场景，避免前端长期停留在 running 状态。
     * 事件会经由 mergedEvents 流到达 subscribeAndAwait 的 onNext，最终写入 SSE 输出。
     * 若框架后续也发出 RunFinished，writeOutgoingEvent 的 terminalEventSent 去重逻辑会兜底。
     */
    fun emitFinish(
        sink: Sinks.Many<AguiEvent>,
        threadId: String?,
        runId: String?
    ) {
        val finish = AguiEvent.RunFinished(threadId, runId)
        emitOrLog(sink, finish, "RunFinished", threadId, runId)
    }

    /**
     * 用 [Sinks.EmitFailureHandler] 包装 emit，避免因瞬时并发未串行化导致丢事件；
     * 出现终止态等不可恢复的失败时记录详细日志。
     */
    private fun emitOrLog(
        sink: Sinks.Many<AguiEvent>,
        event: AguiEvent,
        eventLabel: String,
        threadId: String?,
        runId: String?
    ) {
        try {
            sink.emitNext(event, emitFailureHandler)
        } catch (e: Exception) {
            logger.warn(
                "[SseEventWriter] Failed to emit {} event to sink: threadId={}, runId={}, error={}",
                eventLabel, threadId, runId, e.message
            )
        }
    }

    /** busyLoop 重试 emit 失败的最大时长（毫秒） */
    private const val EMIT_BUSY_LOOP_MILLIS = 100L
}
