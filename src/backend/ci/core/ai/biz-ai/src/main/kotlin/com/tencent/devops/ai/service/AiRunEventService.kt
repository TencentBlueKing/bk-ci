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

import com.tencent.devops.ai.dao.AiAgentStageDao
import com.tencent.devops.ai.dao.AiRunEventDao
import com.tencent.devops.ai.pojo.AiAgentStageMetadata.SessionStatus
import com.tencent.devops.ai.pojo.event.AiRunStopBroadcastEvent
import com.tencent.devops.ai.util.SseEventWriter
import io.agentscope.core.agui.encoder.AguiEventEncoder
import org.glassfish.jersey.server.ChunkedOutput
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * AI 运行事件服务，封装事件持久化与 DB 回放逻辑。
 */
@Service
class AiRunEventService @Autowired constructor(
    private val streamBridge: StreamBridge,
    private val aiRunEventDao: AiRunEventDao,
    private val aiAgentStageDao: AiAgentStageDao,
    private val dslContext: DSLContext,
    private val activeRunManager: ActiveRunManager
) {

    /**
     * 同步持久化事件到 DB。
     * 直接写入，无 MQ / 线程池中间层，保证落库即时、回放零延迟。
     * INSERT IGNORE + UNIQUE 索引保证幂等。
     */
    fun sendPersistEvent(
        threadId: String?,
        runId: String?,
        eventIndex: Int,
        eventData: String
    ) {
        if (!threadId.isNullOrBlank() && !runId.isNullOrBlank()) {
            try {
                aiRunEventDao.create(
                    dslContext = dslContext,
                    threadId = threadId,
                    runId = runId,
                    eventIndex = eventIndex,
                    eventData = eventData
                )
            } catch (e: Exception) {
                logger.warn(
                    "[AiRunEvent] Failed to persist event: " +
                            "threadId={}, runId={}, index={}, error={}",
                    threadId, runId, eventIndex, e.message
                )
            }
        }
    }

    /**
     * 本实例 [ActiveRunManager] 仍持有该 threadId 时，从 [ActiveRunManager.ActiveRun.replaySink]
     * 订阅已缓冲事件并写入 SSE，用于同实例断线重连。
     *
     * @return true 已衔接内存流；false 本实例无活跃运行，调用方可降级 [replayFromDb]
     */
    fun reconnectFromMemory(threadId: String, output: ChunkedOutput<String>): Boolean {
        val activeRun = activeRunManager.get(threadId) ?: return false
        logger.info(
            "[AiRunEvent] Reconnecting from memory: threadId={}, runId={}",
            threadId, activeRun.runId
        )
        val encoder = AguiEventEncoder()
        val latch = CountDownLatch(1)
        var disconnected = false
        activeRun.replaySink.asFlux().subscribe(
            { event ->
                if (!disconnected) {
                    try {
                        output.write(encoder.encode(event))
                    } catch (e: Exception) {
                        disconnected = true
                        logger.info(
                            "[AiRunEvent] Reconnect client disconnected: threadId={}",
                            threadId
                        )
                    }
                }
            },
            { latch.countDown() },
            { latch.countDown() }
        )
        val completed = latch.await(RECONNECT_STREAM_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        if (!completed) {
            logger.warn("[AiRunEvent] Reconnect timeout: threadId={}", threadId)
            SseEventWriter.writeErrorAndFinish(
                output, threadId, activeRun.runId,
                "Reconnect stream timeout after ${RECONNECT_STREAM_TIMEOUT_MINUTES}min"
            )
        }
        return true
    }

    /**
     * 从 DB 回放事件流到 SSE 输出（轮询追踪模式）。
     *
     * 先回放 DB 中已有的事件，然后持续轮询增量事件，
     * 直到检测到流结束（RunFinished 标记）或数据被清理或超时。
     * 解决了"快照回放"模式下 Agent 还在跑但客户端收不到后续事件的问题。
     *
     * @return true 如果找到事件并开始回放，false 如果无事件记录
     */
    fun replayFromDb(
        threadId: String,
        output: ChunkedOutput<String>
    ): Boolean {
        // 先检查是否有事件记录
        if (!aiRunEventDao.existsByRun(dslContext, threadId)) {
            return false
        }

        logger.info("[AiRunEvent] Start polling replay from DB: threadId={}", threadId)

        var lastIndex = -1
        var emptyPollCount = 0
        val startTime = System.currentTimeMillis()

        try {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > REPLAY_POLL_TIMEOUT_MS) {
                    logger.warn("[AiRunEvent] DB replay timeout: threadId={}, elapsed={}ms", threadId, elapsed)
                    SseEventWriter.writeErrorAndFinish(output, threadId, null, "Stream replay timeout")
                    break
                }

                // 增量查询：获取 lastIndex 之后的新事件
                val (events, maxIndex) = aiRunEventDao.listIncrementalByRun(
                    dslContext = dslContext,
                    threadId = threadId,
                    afterIndex = lastIndex
                )

                if (events.isNotEmpty()) {
                    emptyPollCount = 0
                    lastIndex = maxIndex
                    for (eventData in events) {
                        output.write(eventData)
                        // 检测 RunFinished 事件——流结束标记
                        // encode 格式: data: {"type":"RUN_FINISHED",...}\n\n
                        if (eventData.contains(RUN_FINISHED_MARKER)) {
                            logger.info(
                                "[AiRunEvent] DB replay detected RunFinished: threadId={}",
                                threadId
                            )
                            return true
                        }
                    }
                } else {
                    emptyPollCount++
                    // 连续空轮询：检查数据是否已被清理（run 已结束）
                    if (emptyPollCount >= MAX_EMPTY_POLLS) {
                        if (!aiRunEventDao.existsByRun(dslContext, threadId)) {
                            logger.info(
                                "[AiRunEvent] DB replay: data cleaned, run finished: threadId={}",
                                threadId
                            )
                            break
                        }
                        // 重置计数器，继续等待
                        emptyPollCount = 0
                    }
                    // 等待一段时间再轮询
                    Thread.sleep(REPLAY_POLL_INTERVAL_MS)
                }
            }
        } catch (e: Exception) {
            logger.info(
                "[AiRunEvent] Client disconnected during DB replay: threadId={}, error={}",
                threadId, e.message
            )
        }
        return true
    }

    /**
     * 广播 Stop 指令到所有 ai-service 实例。
     * 使用 fanout exchange，每个实例都会收到；RUNNING 阶段的 DB 收尾在 [handleStopBroadcast] 中按事件标志处理。
     */
    fun broadcastStop(
        threadId: String,
        status: SessionStatus = SessionStatus.CANCELLED
    ) {
        try {
            logger.info(
                "[AiRunEvent] Broadcasting stop: threadId={}, status={}",
                threadId, status
            )
            AiRunStopBroadcastEvent(threadId, status).sendTo(streamBridge)
        } catch (e: Exception) {
            logger.warn("[AiRunEvent] Failed to broadcast stop: threadId={}", threadId, e)
        }
    }

    fun handleStopBroadcast(
        event: AiRunStopBroadcastEvent,
        activeRunManager: ActiveRunManager
    ) {
        val activeRun = activeRunManager.get(event.threadId)
        if (activeRun != null) {
            logger.info(
                "[AiRunEvent] Handling stop broadcast: threadId={}, runId={}",
                event.threadId, activeRun.runId
            )
            activeRun.agent.interrupt()
            activeRun.replaySink.tryEmitComplete()
        }
        // 无论本地是否持有 ActiveRun，都执行 remove 以确保 Redis key 被清理。
        // 场景：并发时 cleanup() 已从本地 Map 移除但 Redis 残留，
        // 或多实例部署时 ActiveRun 在其他实例。
        activeRunManager.remove(event.threadId)

        try {
            if (event.status == SessionStatus.TIMEOUT) {
                val updated = aiAgentStageDao.timeoutBySession(dslContext, event.threadId)
                if (updated > 0) {
                    logger.info(
                        "[AiRunEvent] Timeout cleanup: marked {} RUNNING stage(s) as TIMEOUT for threadId={}",
                        updated, event.threadId
                    )
                }
            } else {
                val cancelled = aiAgentStageDao.cancelBySession(dslContext, event.threadId)
                if (cancelled > 0) {
                    logger.info(
                        "[AiRunEvent] Cancelled {} running stage(s): threadId={}",
                        cancelled, event.threadId
                    )
                }
            }
        } catch (e: Exception) {
            val action = if (event.status == SessionStatus.TIMEOUT) "timeout" else "cancel"
            logger.warn("[AiRunEvent] Failed to {} stages: threadId={}", action, event.threadId, e)
        }
    }

    /**
     * 清理事件数据（运行结束后删除）。
     */
    fun sendCleanupEvent(threadId: String?, runId: String?) {
        if (threadId.isNullOrBlank() || runId.isNullOrBlank()) {
            return
        }
        try {
            val deleted = aiRunEventDao.deleteByRun(
                dslContext = dslContext,
                threadId = threadId,
                runId = runId
            )
            logger.info(
                "[AiRunEvent] Cleaned up events: threadId={}, " +
                        "runId={}, deleted={}",
                threadId, runId, deleted
            )
        } catch (e: Exception) {
            logger.warn(
                "[AiRunEvent] Failed to cleanup events: " +
                        "threadId={}, runId={}, error={}",
                threadId, runId, e.message
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AiRunEventService::class.java)

        /** 内存重连时等待 replaySink 流结束的最长时长（分钟） */
        private const val RECONNECT_STREAM_TIMEOUT_MINUTES = 15L

        /** DB 轮询间隔（毫秒） */
        private const val REPLAY_POLL_INTERVAL_MS = 500L

        /** DB 轮询回放最大超时（毫秒），与流超时保持一致：5 分钟 */
        private const val REPLAY_POLL_TIMEOUT_MS = 5 * 60 * 1000L

        /** 连续空轮询次数阈值，超过后检查数据是否已被清理 */
        private const val MAX_EMPTY_POLLS = 10

        /**
         * RunFinished 事件在 SSE 编码后的特征字符串。
         *
         * AguiEventEncoder.encode() 输出格式: data: {"type":"RUN_FINISHED",...}\n\n
         * Jackson @JsonTypeInfo(property="type") + @JsonSubTypes(name="RUN_FINISHED")
         * 决定了序列化后 type 字段值为 "RUN_FINISHED"。
         */
        private const val RUN_FINISHED_MARKER = "\"type\":\"RUN_FINISHED\""
    }
}
