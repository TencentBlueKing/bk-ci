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

package com.tencent.devops.ai.service

import com.tencent.devops.ai.context.AgentSessionContext
import com.tencent.devops.ai.context.AiChatContext
import com.tencent.devops.ai.pojo.ChatContextDTO
import com.tencent.devops.ai.session.PersistentAgentResolver
import com.tencent.devops.ai.util.AguiEventSanitizer
import com.tencent.devops.ai.util.AiErrorMessageTranslator
import com.tencent.devops.ai.util.ReasoningCompensationTracker
import io.agentscope.core.agent.Agent
import io.agentscope.core.agent.AgentBase
import io.agentscope.core.agui.encoder.AguiEventEncoder
import io.agentscope.core.agui.event.AguiEvent
import io.agentscope.core.agui.model.RunAgentInput
import io.agentscope.core.agui.processor.AguiRequestProcessor
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AI 对话核心服务。
 *
 * 负责接收 AG-UI 协议请求、驱动智能体执行、
 * 合并子智能体事件流，并通过 SSE 推送给前端。
 * 通过 [ActiveRunManager] 支持 SSE 断连后重连。
 */
@Service
class AiChatService @Autowired constructor(
    private val processor: AguiRequestProcessor,
    private val resolver: PersistentAgentResolver,
    private val aiSessionService: AiSessionService,
    private val activeRunManager: ActiveRunManager,
    private val sessionContext: AgentSessionContext,
    private val aiRunEventService: AiRunEventService
) {

    /** 缓存 AgentBase.running 字段的反射引用，避免每次重复查找 */
    private val runningFieldCache = ConcurrentHashMap<Class<*>, java.lang.reflect.Field>()

    /** 处理一次 AG-UI 对话请求：初始化会话 → 驱动智能体 → 合并事件流 → SSE 推送 → 清理资源。 */
    fun runChat(
        userId: String,
        input: RunAgentInput,
        output: ChunkedOutput<String>
    ) {
        val threadId = input.threadId
        val runId = input.runId

        logger.info("[AguiChat] Request: userId={}, threadId={}, runId={}", userId, threadId, runId)
        var agent: Agent? = null
        val acquired = threadId.isNullOrBlank() || activeRunManager.tryAcquire(threadId)
        if (!acquired) {
            logger.warn("[AguiChat] Run already active: threadId={}, rejecting", threadId)
            val raw = AguiEvent.Raw(
                input.threadId,
                input.runId,
                mapOf("error" to "当前会话正在进行中，不允许重复发送，待会话结束后重试。")
            )
            output.write(AguiEventEncoder().encode(raw))
            return
        }
        try {
            initContext(userId, input, threadId)
            // 等待框架层 Agent 的 running 标志释放（interrupt 后 releaseExecution 有延迟）
            if (!threadId.isNullOrBlank()) {
                waitForAgentIdle(threadId)
            }
            val result = processor.process(input, null, null)
            agent = result.agent()
            val (activeRun, mergedEvents) = setupEventStream(agent, result, threadId, runId)
            subscribeAndAwait(
                mergedEvents = mergedEvents,
                activeRun = activeRun,
                output = output,
                encoder = AguiEventEncoder(),
                threadId = threadId,
                runId = runId
            )
        } finally {
            cleanup(threadId, agent)
        }
    }

    /** 初始化 ThreadLocal 上下文、确保会话存在并绑定 sessionContext。 */
    private fun initContext(userId: String, input: RunAgentInput, threadId: String?) {
        if (input.hasContext()) {
            val pairs = input.context.map { it.description to it.value }
            AiChatContext.setContext(ChatContextDTO.fromPairs(pairs))
        }
        if (!threadId.isNullOrBlank()) {
            val firstUserMsg = input.messages
                ?.lastOrNull { it.role == ROLE_USER }
                ?.content
            aiSessionService.ensureSession(
                sessionId = threadId,
                userId = userId,
                firstUserMessage = firstUserMsg,
                projectId = AiChatContext.getContext().projectId
            )
            AiChatContext.setThreadId(threadId)
            sessionContext.bindContext(threadId, userId, AiChatContext.getContext())
        }
    }

    /**
     * 创建子智能体 Sink、注册活跃运行，并将主流与子智能体流合并。
     *
     * 同时挂载 [ReasoningCompensationTracker]，在流结束时检测
     * reasoning-only 场景并补发 TEXT_MESSAGE 事件。
     *
     * TODO: 升级 AgentScope 到 1.0.12+ 后可移除补偿逻辑
     *
     * @return 活跃运行实例 与 合并后的事件流
     */
    private fun setupEventStream(
        agent: Agent,
        result: AguiRequestProcessor.ProcessResult,
        threadId: String?,
        runId: String?
    ): Pair<ActiveRunManager.ActiveRun, Flux<AguiEvent>> {
        val subAgentSink = Sinks.many().multicast().onBackpressureBuffer<AguiEvent>()
        sessionContext.registerSink(
            agent,
            AgentSessionContext.SinkInfo(subAgentSink, threadId ?: "", runId ?: "")
        )
        val activeRun = activeRunManager.register(threadId ?: "", runId ?: "", agent)

        val tracker = ReasoningCompensationTracker(threadId ?: "", runId ?: "")
        val mergedEvents = Flux.merge(
            result.events()
                .doOnComplete { subAgentSink.tryEmitComplete() },
            subAgentSink.asFlux()
        ).doOnNext { tracker.track(it) }.concatWith(
            // 补偿逻辑。
            Flux.defer {
                if (tracker.needsCompensation()) {
                    logger.warn(
                        "[AguiChat] Reasoning-only stream detected,compensating TEXT_MESSAGE: threadId={}", threadId
                    )
                    Flux.fromIterable(tracker.buildCompensationEvents())
                } else {
                    Flux.empty()
                }
            })

        return activeRun to mergedEvents
    }

    /** 订阅事件流并写入 SSE，处理断连、错误和超时，阻塞直至流结束。 */
    private fun subscribeAndAwait(
        mergedEvents: Flux<AguiEvent>,
        activeRun: ActiveRunManager.ActiveRun,
        output: ChunkedOutput<String>,
        encoder: AguiEventEncoder,
        threadId: String?,
        runId: String?
    ) {
        val latch = CountDownLatch(1)
        val clientDisconnected = AtomicBoolean(false)
        val terminalEventSent = AtomicBoolean(false)

        mergedEvents.subscribe(
            { event ->
                writeOutgoingEvent(
                    event = event,
                    activeRun = activeRun,
                    output = output,
                    encoder = encoder,
                    threadId = threadId,
                    runId = runId,
                    clientDisconnected = clientDisconnected,
                    terminalEventSent = terminalEventSent
                )
            },
            { error ->
                logger.error("[AguiChat] Stream error: {}", error.message, error)
                writeErrorAndFinish(
                    errorMessage = toFriendlyErrorMessage(error),
                    activeRun = activeRun,
                    output = output,
                    encoder = encoder,
                    threadId = threadId,
                    runId = runId,
                    clientDisconnected = clientDisconnected,
                    terminalEventSent = terminalEventSent
                )
                activeRun.replaySink.tryEmitComplete()
                activeRunManager.remove(threadId ?: "")
                persistAgentState(threadId)
                aiRunEventService.sendCleanupEvent(threadId, runId)
                latch.countDown()
            },
            {
                logger.info("[AguiChat] Stream completed: threadId={}, runId={}", threadId, runId)
                writeRunFinishedIfMissing(
                    reason = "stream completed without terminal event",
                    activeRun = activeRun,
                    output = output,
                    encoder = encoder,
                    threadId = threadId,
                    runId = runId,
                    clientDisconnected = clientDisconnected,
                    terminalEventSent = terminalEventSent
                )
                activeRun.replaySink.tryEmitComplete()
                activeRunManager.remove(threadId ?: "")
                persistAgentState(threadId)
                aiRunEventService.sendCleanupEvent(threadId, runId)
                latch.countDown()
            }
        )

        val completed = latch.await(STREAM_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!completed) {
            logger.warn("[AguiChat] Timeout: threadId={}", threadId)
            writeErrorAndFinish(
                errorMessage = "对话超时（${STREAM_TIMEOUT_MINUTES}分钟），请重新发起。",
                activeRun = activeRun,
                output = output,
                encoder = encoder,
                threadId = threadId,
                runId = runId,
                clientDisconnected = clientDisconnected,
                terminalEventSent = terminalEventSent
            )
            activeRun.replaySink.tryEmitComplete()
            activeRunManager.remove(threadId ?: "")
            aiRunEventService.sendCleanupEvent(threadId, runId)
        }
    }

    /**
     * 统一写出 AG-UI 事件，确保当前 SSE、内存 replay 和 DB replay 使用同一份事件。
     *
     * 多实例部署下，断线重连可能落到其它 ai-service 实例，因此兜底终止事件也必须同步落库。
     */
    private fun writeOutgoingEvent(
        event: AguiEvent,
        activeRun: ActiveRunManager.ActiveRun,
        output: ChunkedOutput<String>,
        encoder: AguiEventEncoder,
        threadId: String?,
        runId: String?,
        clientDisconnected: AtomicBoolean,
        terminalEventSent: AtomicBoolean
    ) {
        val encoded = encoder.encode(event)
        val sanitizedEncoded = AguiEventSanitizer.sanitizeEncodedEvent(encoded)
        if (sanitizedEncoded == null) {
            logger.warn(
                "[AguiChat] Dropped raw tool-call marker event: threadId={}, runId={}",
                threadId, runId
            )
            return
        }
        val duplicatedTerminalEvent = isTerminalEvent(sanitizedEncoded) &&
            !terminalEventSent.compareAndSet(false, true)
        if (duplicatedTerminalEvent) {
            logger.info(
                "[AguiChat] Skip duplicated terminal event: threadId={}, runId={}",
                threadId, runId
            )
            return
        }
        activeRun.replaySink.tryEmitNext(event)
        // 同步持久化到 DB，供多实例重连时从 DB 回放。
        aiRunEventService.sendPersistEvent(
            threadId = threadId,
            runId = runId,
            eventIndex = activeRun.nextEventIndex(),
            eventData = sanitizedEncoded
        )
        if (!clientDisconnected.get()) {
            try {
                output.write(sanitizedEncoded)
            } catch (e: Exception) {
                clientDisconnected.set(true)
                logger.info(
                    "[AguiChat] Client disconnected, agent continues: threadId={}",
                    threadId
                )
            }
        }
    }

    private fun isTerminalEvent(encoded: String): Boolean {
        return encoded.contains(RUN_FINISHED_MARKER) || encoded.contains(RUN_ERROR_MARKER)
    }

    /** 按现有前端契约写出 Raw(error) + RunFinished，并进入 replay/DB 链路。 */
    private fun writeErrorAndFinish(
        errorMessage: String,
        activeRun: ActiveRunManager.ActiveRun,
        output: ChunkedOutput<String>,
        encoder: AguiEventEncoder,
        threadId: String?,
        runId: String?,
        clientDisconnected: AtomicBoolean,
        terminalEventSent: AtomicBoolean
    ) {
        writeOutgoingEvent(
            event = AguiEvent.Raw(threadId, runId, mapOf("error" to errorMessage)),
            activeRun = activeRun,
            output = output,
            encoder = encoder,
            threadId = threadId,
            runId = runId,
            clientDisconnected = clientDisconnected,
            terminalEventSent = terminalEventSent
        )
        writeRunFinishedIfMissing(
            reason = "stream terminated with error",
            activeRun = activeRun,
            output = output,
            encoder = encoder,
            threadId = threadId,
            runId = runId,
            clientDisconnected = clientDisconnected,
            terminalEventSent = terminalEventSent
        )
    }

    /** 框架未发终止事件时兜底补一个 RUN_FINISHED，避免前端长期停留 running。 */
    private fun writeRunFinishedIfMissing(
        reason: String,
        activeRun: ActiveRunManager.ActiveRun,
        output: ChunkedOutput<String>,
        encoder: AguiEventEncoder,
        threadId: String?,
        runId: String?,
        clientDisconnected: AtomicBoolean,
        terminalEventSent: AtomicBoolean
    ) {
        if (terminalEventSent.get()) {
            return
        }
        logger.warn(
            "[AguiChat] Fallback RUN_FINISHED emitted: threadId={}, runId={}, reason={}",
            threadId, runId, reason
        )
        writeOutgoingEvent(
            event = AguiEvent.RunFinished(threadId, runId),
            activeRun = activeRun,
            output = output,
            encoder = encoder,
            threadId = threadId,
            runId = runId,
            clientDisconnected = clientDisconnected,
            terminalEventSent = terminalEventSent
        )
    }

    /** 清理活跃运行、Sink、ThreadLocal 及上下文绑定。 */
    private fun cleanup(threadId: String?, agent: Agent?) {
        if (!threadId.isNullOrBlank()) {
            activeRunManager.remove(threadId)
            sessionContext.evictAll(threadId)
        }
        agent?.let { sessionContext.removeSink(it) }
        AiChatContext.clear()
    }

    /**
     * 重连到正在进行的事件流，回放历史事件后衔接实时事件。
     *
     * 优先从本实例内存回放（replaySink），
     * 若本实例无 ActiveRun 则从 DB 回放（跨实例重连场景）。
     */
    fun reconnectStream(
        threadId: String,
        output: ChunkedOutput<String>
    ): Boolean {
        if (aiRunEventService.reconnectFromMemory(threadId, output)) {
            return true
        }
        logger.info("[AguiChat] Reconnecting from DB: threadId={}", threadId)
        return aiRunEventService.replayFromDb(threadId, output)
    }

    /**
     * 停止正在进行的 Agent 运行。
     *
     * 通过 MQ fanout 广播 Stop 指令到所有 ai-service 实例，
     * 持有该 threadId 运行的实例会执行 interrupt + 关闭 replaySink。
     */
    fun stopRun(threadId: String): Boolean {
        // 广播到所有实例，持有该 session 的实例会执行 interrupt + 标记 CANCELLED
        aiRunEventService.broadcastStop(threadId)
        return true
    }

    fun getActiveRunId(threadId: String): String? = activeRunManager.getRunId(threadId)

    /**
     * 等待框架层 Agent 的 running 标志释放。
     */
    private fun waitForAgentIdle(threadId: String) {
        val agent = resolver.getSessionAgent(threadId) ?: return
        if (agent !is AgentBase) return

        val running = getRunningField(agent) ?: return

        if (!running.get()) return

        logger.info(
            "[AguiChat] Agent still releasing after interrupt, " +
                    "waiting for idle: threadId={}",
            threadId
        )
        val startTime = System.currentTimeMillis()
        while (running.get()) {
            if (System.currentTimeMillis() - startTime > AGENT_IDLE_WAIT_TIMEOUT_MS) {
                logger.warn(
                    "[AguiChat] Timeout waiting for agent idle, " +
                            "force resetting running=false: threadId={}, waited={}ms",
                    threadId, AGENT_IDLE_WAIT_TIMEOUT_MS
                )
                // 强制重置，避免框架 acquireExecution 拒绝新请求
                running.set(false)
                break
            }
            Thread.sleep(AGENT_IDLE_POLL_INTERVAL_MS)
        }
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed > 0) {
            logger.info(
                "[AguiChat] Agent idle wait completed: " +
                        "threadId={}, elapsed={}ms, running={}",
                threadId, elapsed, running.get()
            )
        }
    }

    /**
     * 通过反射获取 [AgentBase] 的 private `running` 字段。
     * 结果缓存在 [runningFieldCache] 中避免重复反射。
     */
    private fun getRunningField(agent: AgentBase): AtomicBoolean? {
        return try {
            val field = runningFieldCache.getOrPut(agent.javaClass) {
                AgentBase::class.java.getDeclaredField("running").apply {
                    isAccessible = true
                }
            }
            field.get(agent) as AtomicBoolean
        } catch (e: Exception) {
            logger.warn(
                "[AguiChat] Failed to access AgentBase.running field: {}",
                e.message
            )
            null
        }
    }

    /** 对话结束后持久化智能体状态，以便下次会话恢复上下文。 */
    private fun persistAgentState(threadId: String?) {
        if (threadId.isNullOrBlank()) return
        try {
            resolver.persistAgent(threadId)
        } catch (e: Exception) {
            logger.warn(
                "[AguiChat] Failed to persist agent state: " +
                        "threadId={}, error={}",
                threadId, e.message
            )
        }
    }

    /**
     * 将框架 / Reactor 异常翻译为用户可读的中文提示。
     * 原始 message 仅保留在后端日志中，不暴露给前端。
     */
    private fun toFriendlyErrorMessage(error: Throwable): String {
        return AiErrorMessageTranslator.toFriendlyMessage(error)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiChatService::class.java)

        /** 单次对话流的最大等待时长（分钟） */
        private const val STREAM_TIMEOUT_MINUTES = 15L
        private const val ROLE_USER = "user"
        private const val RUN_FINISHED_MARKER = "\"type\":\"RUN_FINISHED\""
        private const val RUN_ERROR_MARKER = "\"type\":\"RUN_ERROR\""

        /** 等待框架层 Agent running 标志释放的最大时长（毫秒） */
        private const val AGENT_IDLE_WAIT_TIMEOUT_MS = 5000L

        /** 轮询 Agent running 标志的间隔（毫秒） */
        private const val AGENT_IDLE_POLL_INTERVAL_MS = 200L
    }
}
