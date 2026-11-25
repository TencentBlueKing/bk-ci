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

package com.tencent.devops.process.engine.control

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.util.TaskUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 插件取消信号的索引服务。
 *
 * 构建机心跳 2 秒一次，原先每次心跳都要对 Job 的杀进程队列做一次 RPOP，绝大多数返回空，
 * Redis 上是 O(并发 Job 数 / 2s) 的无效请求。这里用一个共享的 ZSET 索引记录"确实有取消信号"的 Job，
 * 索引在进程内按秒级缓存，心跳先查本地快照，只有命中才去队列真正取数据。
 * 索引的写入发生在队列写入之后，因此索引命中即意味着队列里已有数据。
 *
 * 索引成员在 Job 结束时删除，稳态规模就是"正在取消且尚未结束的 Job"，通常几个到几十个，
 * 因此每秒拉一次全量是划算的：每次刷新都与 Redis 重新对齐，不依赖增量游标，实例重启或漏读都能自愈。
 * 为防清理异常导致的堆积，读取条数有上限，并按分数清理过期残留。
 */
@Component
class CancelTaskSignalService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    /**
     * 本地快照的有效期。正在取消的 Job 数量很少，拉全量成本可忽略，这里控制的是拉取频率。
     */
    @Value("\${pipeline.cancelTaskSignal.snapshotCacheMs:1000}")
    private val snapshotCacheMs: Long = DEFAULT_SNAPSHOT_CACHE_MS

    /**
     * 兜底探测间隔。灰度期共用 Redis 的老版本进程只写队列不写索引，
     * 每个 Job 至少按此间隔查一次队列，保证取消信号不会被索引漏掉。灰度完成后可调大。
     */
    @Value("\${pipeline.cancelTaskSignal.fallbackSweepMs:30000}")
    private val fallbackSweepMs: Long = DEFAULT_FALLBACK_SWEEP_MS

    private val snapshot = AtomicReference(Snapshot(time = 0L, members = emptySet(), available = false))

    private val refreshing = AtomicBoolean(false)

    @Volatile
    private var lastPruneTime = 0L

    private val sweptJobs: Cache<String, Long> = Caffeine.newBuilder()
        .maximumSize(MAX_SWEEP_CACHE_SIZE)
        .expireAfterWrite(SWEEP_RECORD_KEEP_MINUTES, TimeUnit.MINUTES)
        .build()

    /**
     * 取消信号写入队列后登记索引。索引写失败不影响取消本身，兜底探测仍会取到队列数据。
     */
    fun markCancelSignal(buildId: String, containerId: String) {
        val member = TaskUtils.getCancelSignalMember(buildId, containerId)
        try {
            redisOperation.zadd(
                TaskUtils.CANCEL_SIGNAL_INDEX_KEY, member, System.currentTimeMillis().toDouble()
            )
        } catch (ignored: Throwable) {
            LOG.warn("ENGINE|$buildId|CANCEL_SIGNAL|mark index failed|j($containerId)", ignored)
        }
        // 本实例立即可见，避免本地快照窗口内漏掉刚写入的信号
        snapshot.updateAndGet { it.copy(members = it.members + member) }
    }

    /**
     * 取出 [vmSeqId] 这个 Job 上待杀进程的插件 ID；没有取消信号时不访问 Redis，返回 null。
     */
    fun drainCancelTaskIds(buildId: String, vmSeqId: String): MutableSet<String>? {
        if (!needDrain(buildId, vmSeqId)) {
            return null
        }
        val key = TaskUtils.getCancelTaskIdRedisKey(buildId, vmSeqId)
        var cancelTaskIds: MutableSet<String>? = null
        var popped = 0
        while (popped++ < MAX_DRAIN_SIZE) {
            val cancelTaskId = redisOperation.rightPop(key) ?: break
            if (cancelTaskIds == null) {
                cancelTaskIds = mutableSetOf()
            }
            cancelTaskIds.add(cancelTaskId)
        }
        return cancelTaskIds
    }

    private fun needDrain(buildId: String, vmSeqId: String): Boolean {
        val current = currentSnapshot()
        // 索引不可用（未就绪或 Redis 异常）时退回原逻辑，宁可多查也不能漏掉取消
        if (!current.available) {
            return true
        }
        if (current.members.contains(TaskUtils.getCancelSignalMember(buildId, vmSeqId))) {
            return true
        }
        return needFallbackSweep(buildId, vmSeqId)
    }

    private fun needFallbackSweep(buildId: String, vmSeqId: String): Boolean {
        val member = TaskUtils.getCancelSignalMember(buildId, vmSeqId)
        val now = System.currentTimeMillis()
        val lastSweepTime = sweptJobs.getIfPresent(member)
        if (lastSweepTime != null && now - lastSweepTime < fallbackSweepMs) {
            return false
        }
        sweptJobs.put(member, now)
        return true
    }

    private fun currentSnapshot(): Snapshot {
        val current = snapshot.get()
        val now = System.currentTimeMillis()
        if (current.time != 0L && now - current.time < snapshotCacheMs) {
            return current
        }
        // 只让一个线程去刷新，其余线程继续用上一份快照，秒级误差对取消时延无影响
        if (!refreshing.compareAndSet(false, true)) {
            return current
        }
        return try {
            // 取分数最大的若干条，即最近登记的取消。清理异常导致堆积时，
            // 读取量被这个上限挡住；被挡在上限外的旧信号由兜底探测取走，不会丢
            val members = redisOperation.zrevrange(
                TaskUtils.CANCEL_SIGNAL_INDEX_KEY, 0, MAX_SNAPSHOT_SIZE - 1
            ) ?: emptySet()
            // 刷新以 Redis 为准。与 markCancelSignal 并发时本地登记可能被覆盖，
            // 但该信号已写入 Redis 索引，下一次刷新即可见，最多多等一个快照周期
            val fresh = Snapshot(time = now, members = members, available = true)
            snapshot.set(fresh)
            if (members.size >= MAX_SNAPSHOT_SIZE.toInt()) {
                LOG.warn("ENGINE|CANCEL_SIGNAL|index size reached cap|${members.size}, check job cleanup")
            }
            pruneExpiredIfNeeded(now)
            fresh
        } catch (ignored: Throwable) {
            LOG.warn("ENGINE|CANCEL_SIGNAL|load index failed, fallback to queue polling", ignored)
            val degraded = Snapshot(time = now, members = emptySet(), available = false)
            snapshot.set(degraded)
            degraded
        } finally {
            refreshing.set(false)
        }
    }

    /**
     * 索引成员由 Job 结束时清理，这里按分数清掉构建异常残留的脏数据，把索引规模控制在"最近取消"的量级。
     * 过期后该 Job 回落到兜底探测，队列数据仍不会丢。
     */
    private fun pruneExpiredIfNeeded(now: Long) {
        if (now - lastPruneTime < PRUNE_INTERVAL_MS) {
            return
        }
        lastPruneTime = now
        try {
            redisOperation.zremoveRangeByScore(
                TaskUtils.CANCEL_SIGNAL_INDEX_KEY, 0.0, (now - INDEX_ENTRY_TTL_MS).toDouble()
            )
        } catch (ignored: Throwable) {
            LOG.warn("ENGINE|CANCEL_SIGNAL|prune index failed", ignored)
        }
    }

    private data class Snapshot(
        val time: Long,
        val members: Set<String>,
        val available: Boolean
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(CancelTaskSignalService::class.java)
        private const val DEFAULT_SNAPSHOT_CACHE_MS = 1000L
        private const val DEFAULT_FALLBACK_SWEEP_MS = 30000L
        private const val SWEEP_RECORD_KEEP_MINUTES = 10L
        private const val PRUNE_INTERVAL_MS = 60000L
        private const val MAX_SWEEP_CACHE_SIZE = 100000L
        private const val MAX_DRAIN_SIZE = 1000
        private const val MAX_SNAPSHOT_SIZE = 2000L
        private val INDEX_ENTRY_TTL_MS = TimeUnit.HOURS.toMillis(1)
    }
}
