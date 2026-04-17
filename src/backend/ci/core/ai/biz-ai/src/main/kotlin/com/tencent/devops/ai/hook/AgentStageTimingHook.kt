/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.ai.hook

import com.tencent.devops.ai.dao.AiAgentStageDao
import com.tencent.devops.ai.pojo.AiAgentStageMetadata.StageType
import com.tencent.devops.ai.service.AiRunEventService
import com.tencent.devops.ai.util.SseEventWriter
import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.ai.pojo.AiAgentStageMetadata.SessionStatus
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import io.agentscope.core.ReActAgent
import io.agentscope.core.agent.Agent
import io.agentscope.core.hook.ErrorEvent
import io.agentscope.core.hook.Hook
import io.agentscope.core.hook.HookEvent
import io.agentscope.core.hook.PostActingEvent
import io.agentscope.core.hook.PostCallEvent
import io.agentscope.core.hook.PostReasoningEvent
import io.agentscope.core.hook.PostSummaryEvent
import io.agentscope.core.hook.PreActingEvent
import io.agentscope.core.hook.PreCallEvent
import io.agentscope.core.hook.PreReasoningEvent
import io.agentscope.core.hook.PreSummaryEvent
import io.agentscope.core.memory.autocontext.AutoContextConfig
import io.agentscope.core.memory.autocontext.AutoContextMemory
import io.agentscope.core.memory.autocontext.CompressionEvent
import io.agentscope.core.message.MsgRole
import io.agentscope.core.message.ToolUseBlock
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.util.concurrent.ConcurrentHashMap

/**
 * 监听 agentscope Hook，将推理、工具调用、整轮调用、上下文压缩、错误等阶段，用于耗时统计与排障。
 */
@Component
class AgentStageTimingHook @Autowired constructor(
    private val dslContext: DSLContext,
    private val aiAgentStageDao: AiAgentStageDao,
    private val sessionContext: AgentSessionContext,
    private val autoContextConfig: AutoContextConfig,
    private val aiRunEventService: AiRunEventService
) : Hook {

    private data class StageRef(val id: String, val startMs: Long)

    private val reasoningStacks = ConcurrentHashMap<String, ArrayDeque<StageRef>>()
    private val callStacks = ConcurrentHashMap<String, ArrayDeque<StageRef>>()
    private val summaryStacks = ConcurrentHashMap<String, ArrayDeque<StageRef>>()
    private val actingStages = ConcurrentHashMap<String, StageRef>()

    /**
     * 每个 agent 实例已处理的 [CompressionEvent] 偏移量。
     * AutoContextMemory.compressionEvents 是只追加列表，
     * 通过偏移量区分"新增"事件，避免重复写入。
     */
    private val compressionOffsets = ConcurrentHashMap<String, Int>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : HookEvent> onEvent(event: T): Mono<T> {
        evictStaleEntries()
        return when (event) {
            is PreReasoningEvent -> handlePreReasoning(event) as Mono<T>
            is PostReasoningEvent -> handlePostReasoning(event) as Mono<T>
            is PreActingEvent -> handlePreActing(event) as Mono<T>
            is PostActingEvent -> handlePostActing(event) as Mono<T>
            is PreCallEvent -> handlePreCall(event) as Mono<T>
            is PostCallEvent -> handlePostCall(event) as Mono<T>
            is PreSummaryEvent -> handlePreSummary(event) as Mono<T>
            is PostSummaryEvent -> handlePostSummary(event) as Mono<T>
            is ErrorEvent -> handleError(event) as Mono<T>
            else -> Mono.just(event)
        }
    }

    override fun priority(): Int = PRIORITY

    /**
     * 推理阶段开始：插入 RUNNING 状态记录到 T_AI_AGENT_STAGE，
     * 同时估算即将发往 LLM 的请求体大小，便于排查 413 等问题。
     *
     * 注意：本 Hook（priority=50）在 AutoContextHook（priority=0）之后执行，
     * 因此在本方法被调用时，AutoContextMemory.compressIfNeeded() 已经完成，
     * compressionEvents 列表中已包含本轮新增的压缩事件。
     * 通过 [flushCompressionEvents] 检查并写入数据库。
     */
    private fun handlePreReasoning(
        event: PreReasoningEvent
    ): Mono<PreReasoningEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)

        // ── 检查 AutoContextMemory 的压缩事件 ──
        val compressionMono = flushCompressionEvents(event.agent, sessionId)

        val stageId = UUIDUtil.generate()
        val key = stableAgentKey(event)
        reasoningStacks.computeIfAbsent(key) { ArrayDeque() }
            .addLast(StageRef(stageId, System.currentTimeMillis()))
        val diag = estimatePayloadSize(event)
        logger.info(
            "[Timing] LLM调用 START | agent={} | type=Reasoning | {}",
            event.agent.name, diag.summary
        )
        if (diag.totalBytes > PAYLOAD_WARN_BYTES) {
            logger.warn(
                "[Timing] LARGE payload | agent={} | {} | perMsg: {}",
                event.agent.name, diag.summary, diag.perMessageBreakdown()
            )
        }
        val reasoningMono = Mono.fromRunnable<Void> {
            val nextIndex = aiAgentStageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            aiAgentStageDao.create(
                dslContext = dslContext,
                id = stageId,
                sessionId = sessionId,
                agentName = event.agent.name,
                stageIndex = nextIndex,
                stageType = StageType.REASONING.value,
                toolName = event.agent.name,
                toolCallId = event.agent.agentId,
                inputBrief = diag.summary
            )
        }.subscribeOn(Schedulers.boundedElastic())

        return compressionMono
            .then(reasoningMono)
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * 推理阶段结束：计算耗时并更新阶段状态，
     * 同时提取工具调用列表作为输出摘要。
     */
    private fun handlePostReasoning(
        event: PostReasoningEvent
    ): Mono<PostReasoningEvent> {
        val key = stableAgentKey(event)
        val ref = reasoningStacks[key]?.removeLastOrNull()
            ?: return Mono.just(event)
        if (reasoningStacks[key]?.isEmpty() == true) {
            reasoningStacks.remove(key)
        }
        val duration = System.currentTimeMillis() - ref.startMs
        val toolCalls = event.reasoningMessage
            ?.getContentBlocks(ToolUseBlock::class.java)
            ?.map { it.name } ?: emptyList()
        val outputBrief = if (toolCalls.isEmpty()) {
            "final answer"
        } else {
            "toolCalls: ${toolCalls.joinToString(",")}"
        }
        val slowTag = if (duration > LLM_SLOW_THRESHOLD_MS) " [SLOW]" else ""
        logger.info(
            "[Timing] LLM调用 END | agent={} | type=Reasoning | {}ms | {}{}",
            event.agent.name, duration, outputBrief, slowTag
        )
        if (duration > LLM_SLOW_THRESHOLD_MS) {
            logger.warn(
                "[Timing] LLM响应慢 | agent={} | type=Reasoning | {}ms | " +
                        "可能原因: 上下文过大/模型过载/网络抖动",
                event.agent.name, duration
            )
        }
        return Mono.fromRunnable<Void> {
            aiAgentStageDao.finish(
                dslContext = dslContext,
                id = ref.id,
                durationMs = duration,
                outputBrief = outputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * 整轮智能体调用开始（含内部多轮 ReAct），与单次 REASONING 不同。
     */
    private fun handlePreCall(
        event: PreCallEvent
    ): Mono<PreCallEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)
        val stageId = UUIDUtil.generate()
        val key = stableAgentKey(event)
        callStacks.computeIfAbsent(key) { ArrayDeque() }
            .addLast(StageRef(stageId, System.currentTimeMillis()))
        val msgs = event.inputMessages ?: emptyList()
        val inputBrief = buildString {
            append("msgCount=${msgs.size}")
            msgs.lastOrNull { it.role == MsgRole.USER }
                ?.textContent
                ?.take(USER_PREVIEW_CHARS)
                ?.let { append(", lastUserPreview=").append(it) }
        }
        logger.info("[Timing] AgentCall START | agent={}", event.agent.name)
        return Mono.fromRunnable<Void> {
            val nextIndex = aiAgentStageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            aiAgentStageDao.create(
                dslContext = dslContext,
                id = stageId,
                sessionId = sessionId,
                agentName = event.agent.name,
                stageIndex = nextIndex,
                stageType = StageType.AGENT_CALL.value,
                toolName = event.agent.name,
                toolCallId = event.agent.agentId,
                inputBrief = inputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    private fun handlePostCall(
        event: PostCallEvent
    ): Mono<PostCallEvent> {
        val key = stableAgentKey(event)
        val ref = callStacks[key]?.removeLastOrNull()
            ?: return Mono.just(event)
        if (callStacks[key]?.isEmpty() == true) {
            callStacks.remove(key)
        }
        val duration = System.currentTimeMillis() - ref.startMs
        val len = event.finalMessage?.textContent?.length ?: 0
        val outputBrief = "finalLen=$len"
        logger.info(
            "[Timing] AgentCall END | agent={} | duration={}ms",
            event.agent.name, duration
        )
        return Mono.fromRunnable<Void> {
            aiAgentStageDao.finish(
                dslContext = dslContext,
                id = ref.id,
                durationMs = duration,
                outputBrief = outputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * AutoContext 压缩上下文时触发的模型调用（与业务 REASONING 区分）。
     */
    private fun handlePreSummary(
        event: PreSummaryEvent
    ): Mono<PreSummaryEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)
        val stageId = UUIDUtil.generate()
        val key = stableAgentKey(event)
        summaryStacks.computeIfAbsent(key) { ArrayDeque() }
            .addLast(StageRef(stageId, System.currentTimeMillis()))
        val inputBrief =
            "iter=${event.currentIteration}/${event.maxIterations}"
        logger.info(
            "[Timing] LLM调用 START | agent={} | type=压缩 | {}",
            event.agent.name, inputBrief
        )
        return Mono.fromRunnable<Void> {
            val nextIndex = aiAgentStageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            aiAgentStageDao.create(
                dslContext = dslContext,
                id = stageId,
                sessionId = sessionId,
                agentName = event.agent.name,
                stageIndex = nextIndex,
                stageType = StageType.CONTEXT_SUMMARY.value,
                toolName = event.modelName,
                toolCallId = event.agent.agentId,
                inputBrief = inputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    private fun handlePostSummary(
        event: PostSummaryEvent
    ): Mono<PostSummaryEvent> {
        val key = stableAgentKey(event)
        val ref = summaryStacks[key]?.removeLastOrNull()
            ?: return Mono.just(event)
        if (summaryStacks[key]?.isEmpty() == true) {
            summaryStacks.remove(key)
        }
        val duration = System.currentTimeMillis() - ref.startMs
        val summaryLen = event.summaryMessage?.textContent?.length ?: 0

        // 从 AutoContextMemory 获取最新的 CompressionEvent 详情
        val compressionBrief = extractCompressionBrief(event)
        val outputBrief = buildString {
            append("summaryLen=$summaryLen,stop=${event.isStopRequested}")
            if (compressionBrief.isNotEmpty()) {
                append(",$compressionBrief")
            }
        }

        val slowTag = if (duration > LLM_SLOW_THRESHOLD_MS) " [SLOW]" else ""
        logger.info(
            "[Timing] LLM调用 END | agent={} | type=压缩 | " +
                    "{}ms | {}{}",
            event.agent.name, duration, outputBrief, slowTag
        )
        if (duration > LLM_SLOW_THRESHOLD_MS) {
            logger.warn(
                "[Timing] LLM响应慢 | agent={} | type=压缩 | {}ms",
                event.agent.name, duration
            )
        }
        return Mono.fromRunnable<Void> {
            aiAgentStageDao.finish(
                dslContext = dslContext,
                id = ref.id,
                durationMs = duration,
                outputBrief = outputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * 从 Agent 的 AutoContextMemory 中提取**本轮新增**的 [CompressionEvent]，
     * 组装为简要字符串用于日志和数据库记录。
     *
     * 通过 [compressionOffsets] 记录每个 Agent 上次读取的偏移量，
     * 避免重复记录历史事件。
     */
    private fun extractCompressionBrief(event: PostSummaryEvent): String {
        val agent = event.agent as? ReActAgent ?: return ""
        val memory = agent.memory as? AutoContextMemory ?: return ""
        val events: List<CompressionEvent> = try {
            memory.compressionEvents
        } catch (_: Exception) {
            return ""
        }
        if (events.isEmpty()) return ""
        val key = stableAgentKey(event)
        val offset = compressionOffsets.getOrDefault(key, 0)
        if (offset >= events.size) return ""
        val newEvents = events.subList(offset, events.size)
        compressionOffsets[key] = events.size
        return newEvents.joinToString("; ") { e ->
            "type=${e.eventType}" +
                    ",in=${e.compressInputToken}" +
                    ",out=${e.compressOutputToken}" +
                    ",msgs=${e.compressedMessageCount}"
        }
    }

    /**
     * 检查 AutoContextMemory 中是否有新增的 [CompressionEvent]，
     * 如果有则为每个事件写入一条 [com.tencent.devops.ai.pojo.AiAgentStageMetadata.StageType.CONTEXT_SUMMARY] 记录到数据库。
     */
    private fun flushCompressionEvents(agent: Agent, sessionId: String): Mono<Void> {
        val reactAgent = agent as? ReActAgent ?: return Mono.empty()
        val memory = reactAgent.memory as? AutoContextMemory ?: return Mono.empty()
        val events: List<CompressionEvent> = try {
            memory.compressionEvents
        } catch (_: Exception) {
            return Mono.empty()
        }
        val key = "${agent.name}@${System.identityHashCode(agent)}"
        val offset = compressionOffsets.getOrDefault(key, 0)
        if (offset >= events.size) return Mono.empty()
        val newEvents = events.subList(offset, events.size)
        compressionOffsets[key] = events.size

        logger.info(
            "[Timing] Flushing {} AutoContext compression event(s) | agent={}",
            newEvents.size, agent.name
        )

        return Mono.fromRunnable<Void> {
            newEvents.forEach { e ->
                val stageId = UUIDUtil.generate()
                val nextIndex = aiAgentStageDao.getMaxIndex(dslContext, sessionId) + 1
                val brief = "type=${e.eventType}" +
                        ",in=${e.compressInputToken}" +
                        ",out=${e.compressOutputToken}" +
                        ",msgs=${e.compressedMessageCount}"
                aiAgentStageDao.create(
                    dslContext = dslContext,
                    id = stageId,
                    sessionId = sessionId,
                    agentName = agent.name,
                    stageIndex = nextIndex,
                    stageType = StageType.CONTEXT_SUMMARY.value,
                    toolName = e.eventType,
                    toolCallId = agent.agentId,
                    inputBrief = brief
                )
                val durationMs = if (e.compressInputToken > 0) {
                    (e.timestamp - (events.getOrNull(offset)?.timestamp ?: e.timestamp))
                } else 0L
                logger.info(
                    "[Timing] ContextCompression | agent={} | type={} | " +
                            "tokenBefore={} | tokenAfter={} | reduction={} | duration={}ms",
                    agent.name, e.eventType,
                    e.tokenBefore, e.tokenAfter, e.tokenReduction, durationMs
                )
                aiAgentStageDao.finish(
                    dslContext = dslContext,
                    id = stageId,
                    durationMs = durationMs,
                    outputBrief = brief
                )
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * 框架错误：无 Pre 配对，直接写入一条 ERROR 阶段。
     *
     * 特殊处理 LLM 超时异常：JDK HttpClient 抛出 [HttpTimeoutException]，
     * 或经 Socket 层抛出 [SocketTimeoutException]，SDK 会包装为
     * HttpTransportException 再通过 ErrorEvent 传递。
     */
    private fun handleError(
        event: ErrorEvent
    ): Mono<ErrorEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent) ?: return Mono.just(event)
        val err = event.error
        val timeout = findTimeoutCause(err)
        val brief = if (timeout != null) {
            "LLM_TIMEOUT(${timeout.javaClass.simpleName}): ${timeout.message ?: err.message ?: ""}"
        } else {
            "${err.javaClass.simpleName}: ${err.message ?: ""}"
        }
        if (timeout != null) {
            logger.error(
                "[Timing] ErrorEvent LLM超时 | agent={} | {} | " +
                        "readTimeout={}s | 可能原因: 模型过载/上下文过大/网络抖动",
                event.agent.name, brief, autoContextConfig.maxToken, err
            )
        } else {
            logger.error(
                "[Timing] ErrorEvent | agent={} | {}",
                event.agent.name, brief, err
            )
        }
        val status = if (timeout != null) {
            SessionStatus.TIMEOUT.value
        } else {
            SessionStatus.ERROR.value
        }
        return Mono.fromRunnable<Void> {
            val nextIndex = aiAgentStageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            val id = UUIDUtil.generate()
            aiAgentStageDao.create(
                dslContext = dslContext,
                id = id,
                sessionId = sessionId,
                agentName = event.agent.name,
                stageIndex = nextIndex,
                stageType = StageType.ERROR.value,
                toolName = event.agent.name,
                toolCallId = event.agent.agentId,
                inputBrief = brief
            )
            val stackHead = err.stackTrace
                .take(ERROR_STACK_LINES)
                .joinToString(" | ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
            aiAgentStageDao.finish(
                dslContext = dslContext,
                id = id,
                durationMs = 0L,
                sessionStatus = status,
                outputBrief = stackHead
            )
            if (timeout != null) {
                cleanupOnTimeout(sessionId)
            }
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * LLM 超时后：
     * 1. 通过 Sink 向前端推送超时错误事件（Raw + RunFinished）
     * 2. 广播 Stop（[AiRunEventService.broadcastStop] 含 MQ + 本实例将 RUNNING 标为 TIMEOUT），
     *    各实例 [AiRunEventService.handleStopBroadcast] 再完成 interrupt、ActiveRun 清理等。
     */
    private fun cleanupOnTimeout(sessionId: String) {
        val sinkInfo = sessionContext.getSinkByThreadId(sessionId)
        if (sinkInfo != null) {
            SseEventWriter.emitErrorAndFinish(
                sinkInfo.sink,
                sinkInfo.threadId,
                sinkInfo.runId,
                "LLM request timeout"
            )
        }
        aiRunEventService.broadcastStop(sessionId, SessionStatus.TIMEOUT)
    }

    /**
     * 沿异常 cause 链查找超时异常。
     * JDK HttpClient 超时 → [HttpTimeoutException]；
     * Socket 层超时 → [SocketTimeoutException]。
     * SDK 的 HttpTransportException 会将原始超时异常作为 cause 包装。
     */
    private fun findTimeoutCause(err: Throwable): Throwable? {
        var current: Throwable? = err
        val visited = mutableSetOf<Throwable>()
        while (current != null && visited.add(current)) {
            if (current is HttpTimeoutException || current is SocketTimeoutException) {
                return current
            }
            current = current.cause
        }
        return null
    }

    /**
     * 工具调用开始：插入 RUNNING 状态记录到 T_AI_AGENT_STAGE，
     * 同时记录 LLM 传递的完整入参。
     */
    private fun handlePreActing(
        event: PreActingEvent
    ): Mono<PreActingEvent> {
        val sessionId = sessionContext.getThreadIdByAgent(event.agent)
            ?: return Mono.just(event)
        val toolName = event.toolUse.name
        val toolId = event.toolUse.id
        val toolInput = event.toolUse.input
        val stageId = UUIDUtil.generate()
        val key = actingKey(event, toolId)
        actingStages[key] = StageRef(
            stageId, System.currentTimeMillis()
        )
        val inputPreview = toolInput.takeIf { it.isNotEmpty() }
            ?.toString()?.take(200) ?: ""
        logger.info(
            "[Timing] Tool START | agent={} | tool={} | input={}",
            event.agent.name, toolName, inputPreview
        )
        return Mono.fromRunnable<Void> {
            val nextIndex = aiAgentStageDao.getMaxIndex(
                dslContext, sessionId
            ) + 1
            aiAgentStageDao.create(
                dslContext = dslContext,
                id = stageId,
                sessionId = sessionId,
                agentName = event.agent.name,
                stageIndex = nextIndex,
                stageType = StageType.TOOL_CALL.value,
                toolName = toolName,
                toolCallId = toolId,
                inputBrief = toolInput.takeIf { it.isNotEmpty() }?.toString()
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * 工具调用结束：计算耗时并更新阶段状态，
     * 超过 [SLOW_THRESHOLD_MS] 时输出慢调用告警。
     * 检测工具执行失败时输出包含入参的 ERROR 日志（切面式诊断）。
     */
    private fun handlePostActing(
        event: PostActingEvent
    ): Mono<PostActingEvent> {
        val toolName = event.toolUse.name
        val toolId = event.toolUse.id
        val toolInput = event.toolUse.input
        val key = actingKey(event, toolId)
        val ref = actingStages.remove(key)
            ?: return Mono.just(event)
        val duration = System.currentTimeMillis() - ref.startMs
        val outputBlocks = event.toolResult?.output ?: emptyList()
        val resultLen = outputBlocks.size

        val resultText = outputBlocks
            .filterIsInstance<io.agentscope.core.message.TextBlock>()
            .joinToString("") { it.text }
        val resultChars = resultText.length
        val outputBrief = "blocks=$resultLen,chars=$resultChars"

        val failed = isToolFailed(resultText)
        val status = if (failed) "FAILED" else "OK"
        val slowTag = if (duration > SLOW_THRESHOLD_MS) " [SLOW]" else ""

        logger.info(
            "[Timing] Tool END | agent={} | tool={} | {}ms | {} | chars={}{}",
            event.agent.name, toolName, duration, status, resultChars, slowTag
        )

        if (resultChars > TOOL_RESULT_WARN_CHARS) {
            logger.warn(
                "[Timing] LARGE tool result | agent={} | tool={} | chars={} | KB={}",
                event.agent.name, toolName, resultChars, resultChars / 1024
            )
        }

        if (failed) {
            logger.error(
                "[Timing] Tool FAILED | agent={} | tool={} | {}ms | input={} | result={}",
                event.agent.name, toolName, duration, toolInput, resultText
            )
        }
        val finishStatus = if (failed) {
            SessionStatus.ERROR.value
        } else {
            SessionStatus.SUCCESS.value
        }

        val finishOutputBrief = if (failed) extractErrorText(resultText) else outputBrief
        return Mono.fromRunnable<Void> {
            aiAgentStageDao.finish(
                dslContext = dslContext,
                id = ref.id,
                durationMs = duration,
                sessionStatus = finishStatus,
                outputBrief = finishOutputBrief
            )
        }.subscribeOn(Schedulers.boundedElastic())
            .thenReturn(event)
            .onErrorReturn(event)
    }

    /**
     * 判断工具执行结果是否为失败。
     *
     * 需覆盖两类错误：
     * 1. **框架级错误** — agentscope 参数校验失败时直接返回 "Error: ..."（无 JSON 引号包裹）
     * 2. **业务级错误** — safeQuery/safeOperate catch 异常后返回的 "查询失败: ..."/"操作失败: ..."
     *    （经 DefaultToolResultConverter JSON 序列化后会被双引号包裹）
     */
    private fun isToolFailed(resultText: String): Boolean {
        if (resultText.startsWith("Error:")) return true
        val text = resultText.removeSurrounding("\"")
        return text.startsWith("查询失败:") || text.startsWith("操作失败:")
    }

    /**
     * 从 resultText 中提取干净的错误文本（去掉 JSON 引号），用于存入数据库。
     */
    private fun extractErrorText(resultText: String): String {
        return resultText.removeSurrounding("\"")
    }

    private fun stableAgentKey(event: HookEvent): String {
        return "${event.agent.name}@${System.identityHashCode(event.agent)}"
    }

    private fun actingKey(
        event: HookEvent,
        toolId: String
    ): String {
        return "${event.agent.name}:$toolId"
    }

    /**
     * 清理超过 [STALE_THRESHOLD_MS] 的残留条目。
     * 当 Agent 被 interrupt 或流异常中断时，Post 事件不触发，
     * Pre 写入的条目会永久残留。借每次 onEvent 触发时顺带清理。
     */
    private fun evictStaleEntries() {
        val now = System.currentTimeMillis()
        evictDequeMap(reasoningStacks, now, "reasoning")
        evictDequeMap(callStacks, now, "agentCall")
        evictDequeMap(summaryStacks, now, "contextSummary")
        actingStages.entries.removeIf { entry ->
            (now - entry.value.startMs > STALE_THRESHOLD_MS).also {
                if (it) logger.warn(
                    "[Timing] Evicting stale acting: " +
                            "key={}, age={}ms",
                    entry.key,
                    now - entry.value.startMs
                )
            }
        }
        if (compressionOffsets.size > COMPRESSION_OFFSETS_EVICT_THRESHOLD) {
            val activeKeys = buildSet {
                addAll(reasoningStacks.keys)
                addAll(callStacks.keys)
                addAll(summaryStacks.keys)
            }
            compressionOffsets.keys.removeIf { it !in activeKeys }
        }
    }

    private fun evictDequeMap(
        map: ConcurrentHashMap<String, ArrayDeque<StageRef>>,
        now: Long,
        label: String
    ) {
        map.keys.toList().forEach { key ->
            val deque = map[key] ?: return@forEach
            deque.removeAll { ref ->
                val expired = now - ref.startMs > STALE_THRESHOLD_MS
                if (expired) {
                    logger.warn(
                        "[Timing] Evicting stale {} stage: " +
                                "key={}, stageId={}, age={}ms",
                        label, key, ref.id, now - ref.startMs
                    )
                }
                expired
            }
            if (deque.isEmpty()) {
                map.remove(key)
            }
        }
    }

    // ==================== Payload size estimation ====================

    private data class PayloadDiag(
        val totalBytes: Long,
        val messagesBytes: Long,
        val toolsBytes: Long,
        val toolCount: Int,
        val msgCount: Int,
        val sysPromptChars: Int,
        val sdkEstTokens: Long,
        val summary: String,
        private val messages: List<io.agentscope.core.message.Msg>
    ) {
        fun perMessageBreakdown(): String = messages.mapIndexed { idx, msg ->
            val chars = msg.textContent?.length ?: 0
            "[$idx]${msg.role}=${(chars / 2.5).toInt()}tk"
        }.joinToString(", ")
    }

    /**
     * 估算即将发往 LLM 的请求体大小。
     *
     * 输出两类指标：
     * - **jsonBytes**：JSON 序列化字节数，用于排查 413 "Request body too large"
     * - **sdkEstTokens**：与 SDK [AutoContextMemory] 对齐的 token 估算
     *   （sum of textContent.length / 2.5），可直接对比压缩触发阈值
     */
    private fun estimatePayloadSize(event: PreReasoningEvent): PayloadDiag {
        val messages = event.inputMessages
        val messagesBytes = try {
            JsonUtil.toJson(messages)
                .toByteArray(Charsets.UTF_8).size.toLong()
        } catch (_: Exception) {
            -1L
        }

        var toolsBytes = 0L
        var toolCount = 0
        val agent = event.agent
        if (agent is ReActAgent) {
            try {
                val schemas = agent.toolkit?.getToolSchemas() ?: emptyList()
                toolCount = schemas.size
                toolsBytes = JsonUtil.toJson(schemas).toByteArray(Charsets.UTF_8).size.toLong()
            } catch (_: Exception) { /* ignore */
            }
        }

        val sysPromptChars = messages
            .firstOrNull { it.role == MsgRole.SYSTEM }
            ?.textContent?.length ?: 0

        val totalBytes = maxOf(messagesBytes, 0) + toolsBytes

        val msgsTextChars = messages.sumOf { it.textContent?.length ?: 0 }.toLong()
        val sdkEstTokens = (msgsTextChars / 2.5).toLong()
        val toolsEstTokens = (toolsBytes / 2.5).toLong()
        val totalEstTokens = sdkEstTokens + toolsEstTokens

        val tokenThreshold = (autoContextConfig.maxToken * autoContextConfig.tokenRatio).toLong()
        val msgThreshold = autoContextConfig.msgThreshold
        val contextWindow = autoContextConfig.maxToken

        val ctxPercent = if (contextWindow > 0) totalEstTokens * 100 / contextWindow else 0

        val summary = "消息=${sdkEstTokens}tk, " +
                "工具=${toolsEstTokens}tk, " +
                "消息数=${messages.size} | " +
                "压缩触发: ${sdkEstTokens}/${tokenThreshold}tk, " +
                "${messages.size}/${msgThreshold}条 | " +
                "上下文: ${totalEstTokens}/${contextWindow}tk(${ctxPercent}%)"

        return PayloadDiag(
            totalBytes = totalBytes,
            messagesBytes = messagesBytes,
            toolsBytes = toolsBytes,
            toolCount = toolCount,
            msgCount = messages.size,
            sysPromptChars = sysPromptChars,
            sdkEstTokens = sdkEstTokens,
            summary = summary,
            messages = messages
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            AgentStageTimingHook::class.java
        )
        private const val PRIORITY = 50
        private const val SLOW_THRESHOLD_MS = 3000L

        /** LLM 调用（Reasoning/压缩）超过 30 秒视为慢响应 */
        private const val LLM_SLOW_THRESHOLD_MS = 30_000L

        /** 超过 30 分钟未完成的阶段视为残留 */
        private const val STALE_THRESHOLD_MS = 30 * 60 * 1000L

        /**
         * Kimi K2.5 上下文 262K tokens ≈ 655KB，
         * 512KB（≈ 200K tokens）足以在接近上限前预警。
         */
        private const val PAYLOAD_WARN_BYTES = 512L * 1024

        /** 单个工具返回超过 50K 字符时输出 WARN 日志 */
        private const val TOOL_RESULT_WARN_CHARS = 50_000

        private const val USER_PREVIEW_CHARS = 300

        private const val ERROR_STACK_LINES = 5

        /** compressionOffsets 超过此大小时触发清理不再活跃的条目 */
        private const val COMPRESSION_OFFSETS_EVICT_THRESHOLD = 100
    }
}
