/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.ai.pojo.AiAgentStageMetadata.SessionStatus
import com.tencent.devops.common.redis.RedisOperation
import io.agentscope.core.agent.Agent
import io.agentscope.core.agui.event.AguiEvent
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * 管理进行中的 Agent 运行实例。
 *
 * - [activeRuns]：本地 [ConcurrentHashMap]，维护 [ActiveRun]（含 replaySink 用于 SSE 重连）
 * - Redis：跨实例活跃状态（[isActive] / [tryAcquire] / [getRunId]），避免多实例并发处理同一 threadId
 * - [pendingStops]：暂存"早到的 stop"，解决 stop 在 [register] 之前到达时的信号丢失，详见字段注释
 */
@Component
class ActiveRunManager(
    private val redisOperation: RedisOperation
) {
    private fun redisKey(threadId: String) = "$REDIS_KEY_PREFIX$threadId"
    private fun lockKey(threadId: String) = "$REDIS_LOCK_PREFIX$threadId"

    /**
     * @param threadId 会话 ID
     * @param runId 本次运行 ID
     * @param agent Agent 实例引用
     * @param replaySink replay sink，保留所有已发出事件供重连回放
     * @param output 当前 SSE 长连接的 ChunkedOutput，供外部线程（如 stop 广播处理器）
     *               同步直写终止事件，绕开 reactor 异步链路与客户端关连接的赛跑
     * @param terminalEventSent 终止事件去重标志，供 subscribeAndAwait 与 handleStopBroadcast 共享
     * @param clientDisconnected 客户端断连标志，避免对已断开连接重复 write 抛错
     * @param eventIndexCounter 事件序号计数器，线程安全自增，保证单 run 内事件顺序
     * @param startTime 运行开始时间戳
     */
    data class ActiveRun(
        val threadId: String,
        val runId: String,
        val agent: Agent,
        val replaySink: Sinks.Many<AguiEvent>,
        val output: ChunkedOutput<String>,
        val terminalEventSent: AtomicBoolean = AtomicBoolean(false),
        val clientDisconnected: AtomicBoolean = AtomicBoolean(false),
        val eventIndexCounter: AtomicInteger = AtomicInteger(0),
        val startTime: Long = System.currentTimeMillis()
    ) {
        /** 获取下一个事件序号（线程安全） */
        fun nextEventIndex(): Int = eventIndexCounter.getAndIncrement()
    }

    private val activeRuns = ConcurrentHashMap<String, ActiveRun>()

    /**
     * 暂存"早到的 stop"请求：stop 在 [register] 之前到达时，[get] 返回 null 会让 stop 信号丢失，
     * agent 跑完整次回答前端才能收到 RUN_FINISHED。
     *
     * 链路：[recordPendingStop] 留痕 → runChat 在 [register] 后 [consumePendingStop] 触发同步终止；
     * cleanup 用 [discardPendingStop] 兜底；[PENDING_STOP_TTL_MS] 防跨实例残留。
     *
     * 注意 [remove] 不能顺手清，否则会被 [AiRunEventService.handleStopBroadcast] 盲调时擦掉刚留的便条。
     */
    private val pendingStops = ConcurrentHashMap<String, PendingStop>()

    /** 早到 stop 请求的暂存项，配合 [PENDING_STOP_TTL_MS] 判定是否仍然有效。 */
    data class PendingStop(
        val status: SessionStatus,
        val timestamp: Long
    )

    /**
     * 注册一个新的活跃运行，创建 replay sink 用于事件缓冲。
     *
     * 调用方拿到 [ActiveRun] 后应紧跟一次 [consumePendingStop]，处理早到 stop。详见 [pendingStops]。
     *
     * @return 已注册的 [ActiveRun]，调用方通过 [ActiveRun.replaySink] 推送事件
     */
    fun register(
        threadId: String,
        runId: String,
        agent: Agent,
        output: ChunkedOutput<String>
    ): ActiveRun {
        val sink = Sinks.many().replay().all<AguiEvent>()
        val run = ActiveRun(threadId, runId, agent, sink, output)
        activeRuns[threadId] = run
        try {
            redisOperation.set(redisKey(threadId), runId, REDIS_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis set failed: threadId={}", threadId, e)
        }
        logger.info(
            "[ActiveRun] Registered: threadId={}, runId={}",
            threadId, runId
        )
        return run
    }

    fun get(threadId: String): ActiveRun? {
        return activeRuns[threadId]
    }

    fun remove(threadId: String) {
        activeRuns.remove(threadId)
        try {
            redisOperation.delete(redisKey(threadId))
            redisOperation.delete(lockKey(threadId))
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis delete failed: threadId={}", threadId, e)
        }
        logger.info(
            "[ActiveRun] Removed: threadId={}, remaining={}",
            threadId, activeRuns.size
        )
    }

    /**
     * 显式丢弃 pendingStop。仅在会话彻底结束（runChat finally → cleanup）时调用，
     * 避免 [consumePendingStop] 漏跑（如 register 之前就抛异常）导致 entry 残留。
     *
     * 注意：正常 happy path 下 [consumePendingStop] 已经 remove，这里通常 noop。
     */
    fun discardPendingStop(threadId: String) {
        if (threadId.isBlank()) return
        pendingStops.remove(threadId)
    }

    /**
     * 记录早到的 stop 请求。同 threadId 重复调用以最后一次为准
     *（本地直连 + MQ self-loop 都会进来，覆盖语义无害）。详见 [pendingStops]。
     */
    fun recordPendingStop(threadId: String, status: SessionStatus) {
        if (threadId.isBlank()) return
        pendingStops[threadId] = PendingStop(status, System.currentTimeMillis())
        logger.info(
            "[ActiveRun] Recorded pending stop: threadId={}, status={}",
            threadId, status
        )
    }

    /**
     * 取出并清除早到的 stop 请求。详见 [pendingStops]。
     *
     * @return 仍在 [PENDING_STOP_TTL_MS] 内的有效 [PendingStop]；不存在或已过期返回 null
     */
    fun consumePendingStop(threadId: String): PendingStop? {
        if (threadId.isBlank()) return null
        val pending = pendingStops.remove(threadId) ?: return null
        if (System.currentTimeMillis() - pending.timestamp > PENDING_STOP_TTL_MS) {
            logger.info(
                "[ActiveRun] Pending stop expired, ignored: threadId={}, ageMs={}",
                threadId, System.currentTimeMillis() - pending.timestamp
            )
            return null
        }
        return pending
    }

    /**
     * 跨实例检查是否有活跃运行：先查本地，再查 Redis。
     */
    fun isActive(threadId: String): Boolean {
        if (activeRuns.containsKey(threadId)) return true
        return try {
            redisOperation.get(redisKey(threadId)) != null
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis get failed: threadId={}", threadId, e)
            false
        }
    }

    /**
     * 原子性地尝试占位：若当前 threadId 无活跃运行则占位成功，否则失败。
     */
    fun tryAcquire(threadId: String): Boolean {
        if (activeRuns.containsKey(threadId)) return false
        return try {
            redisOperation.setIfAbsent(lockKey(threadId), "1", REDIS_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis setIfAbsent failed: threadId={}", threadId, e)
            true
        }
    }

    /**
     * 跨实例获取活跃运行的 runId：先查本地，再查 Redis。
     * @return runId，无活跃运行时返回 null
     */
    fun getRunId(threadId: String): String? {
        activeRuns[threadId]?.let { return it.runId }
        return try {
            redisOperation.get(redisKey(threadId))
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis get failed: threadId={}", threadId, e)
            null
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            ActiveRunManager::class.java
        )

        private const val REDIS_KEY_PREFIX = "ai:active_run:"
        private const val REDIS_LOCK_PREFIX = "ai:active_run_lock:"

        /** Redis TTL 与本地残留阈值对齐，自动过期兜底 */
        private const val REDIS_TTL_SECONDS = 30 * 60L

        // pending stop 的最大有效期。
        private const val PENDING_STOP_TTL_MS = 30 * 1000L
    }
}
