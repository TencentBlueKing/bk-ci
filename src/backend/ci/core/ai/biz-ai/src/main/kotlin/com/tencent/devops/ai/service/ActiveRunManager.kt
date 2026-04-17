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

import com.tencent.devops.common.redis.RedisOperation
import io.agentscope.core.agent.Agent
import io.agentscope.core.agui.event.AguiEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 管理进行中的 Agent 运行实例。
 *
 * 本地使用 [ConcurrentHashMap] 存储 [ActiveRun]（含 replaySink 用于 SSE 重连），
 * 同时通过 Redis 实现跨实例的活跃状态检查，避免多实例部署时同一 threadId 被重复处理。
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
     * @param eventIndexCounter 事件序号计数器，线程安全自增，保证单 run 内事件顺序
     * @param startTime 运行开始时间戳
     */
    data class ActiveRun(
        val threadId: String,
        val runId: String,
        val agent: Agent,
        val replaySink: Sinks.Many<AguiEvent>,
        val eventIndexCounter: AtomicInteger = AtomicInteger(0),
        val startTime: Long = System.currentTimeMillis()
    ) {
        /** 获取下一个事件序号（线程安全） */
        fun nextEventIndex(): Int = eventIndexCounter.getAndIncrement()
    }

    private val activeRuns = ConcurrentHashMap<String, ActiveRun>()

    /**
     * 注册一个新的活跃运行，创建 replay sink 用于事件缓冲。
     *
     * @return 已注册的 [ActiveRun]，调用方通过
     *         [ActiveRun.replaySink] 推送事件
     */
    fun register(
        threadId: String,
        runId: String,
        agent: Agent
    ): ActiveRun {
        val sink = Sinks.many().replay().all<AguiEvent>()
        val run = ActiveRun(threadId, runId, agent, sink)
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
        evictStaleEntries()
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
     * 跨实例检查是否有活跃运行：先查本地，再查 Redis。
     */
    fun isActive(threadId: String): Boolean {
        evictStaleEntries()
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
        evictStaleEntries()
        activeRuns[threadId]?.let { return it.runId }
        return try {
            redisOperation.get(redisKey(threadId))
        } catch (e: Exception) {
            logger.warn("[ActiveRun] Redis get failed: threadId={}", threadId, e)
            null
        }
    }

    /**
     * 清理超过 [STALE_THRESHOLD_MS] 的残留条目。
     * 正常路径下 onComplete/onError/finally 会清理，
     * 但极端情况（如 subscribe 前异常且 finally 未覆盖）
     * 可能导致条目残留，此处做兜底防护。
     */
    private fun evictStaleEntries() {
        val now = System.currentTimeMillis()
        activeRuns.entries.removeIf { entry ->
            val stale = now - entry.value.startTime > STALE_THRESHOLD_MS
            if (stale) {
                entry.value.replaySink.tryEmitComplete()
                try {
                    redisOperation.delete(redisKey(entry.key))
                    redisOperation.delete(lockKey(entry.key))
                } catch (_: Exception) { /* best effort */
                }
                logger.warn(
                    "[ActiveRun] Evicting stale entry: threadId={}, age={}ms",
                    entry.key, now - entry.value.startTime
                )
            }
            stale
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(
            ActiveRunManager::class.java
        )

        /** 超过 30 分钟未完成的运行视为残留 */
        private const val STALE_THRESHOLD_MS = 30 * 60 * 1000L

        private const val REDIS_KEY_PREFIX = "ai:active_run:"
        private const val REDIS_LOCK_PREFIX = "ai:active_run_lock:"

        /** Redis TTL 与本地残留阈值对齐，自动过期兜底 */
        private const val REDIS_TTL_SECONDS = 30 * 60L
    }
}
