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
 * 插件取消信号的索引。心跳用它判断要不要去读杀进程队列，队列本身的内容和消费方式不变。
 *
 * 构建机每 2 秒心跳一次。原先每次心跳都对 `CANCEL_TASK_IDS_{buildId}_{jobId}` 做 RPOP，
 * 取消是极少发生的事，这些 RPOP 几乎全部返回空，调用量随并发 Job 数线性涨。
 *
 * 数据放在所有实例共用的一个 ZSET 里，Key 是 [TaskUtils.CANCEL_SIGNAL_INDEX_KEY]：
 * - member 是 `buildId:jobId`，一个 Job 一条，重复取消只刷新分数
 * - score 是登记时的毫秒时间戳，供过期清理和"只取最近的若干条"使用
 * 写入在杀进程队列 LPUSH 之后（[com.tencent.devops.process.engine.service.PipelineBuildTaskService.finishTask]），
 * 所以索引里出现这个 Job 时，队列里当时一定已经有数据。
 * 删除在 Job 结束和同 buildId 重试前（[TaskUtils.clearJobCancelFlag]、[TaskUtils.clearBuildJobCancelFlags]）。
 * 因此稳态下成员数等于"正在取消、还没结束的 Job"，通常是几个到几十个。
 *
 * 每个进程把这张索引缓存在本地快照里，默认 1 秒刷新一次，用 ZREVRANGE 取分数最高的 [MAX_SNAPSHOT_SIZE] 条。
 * 快照是缓存，不是数据源。取消写在实例 A、心跳打到实例 B 时，B 要等自己这份快照过期后重新拉取才能看见，
 * 默认最多 1 秒。构建机心跳 2 秒一次，所以构建机侧再叠加最多一个心跳间隔。
 * 每次刷新都整份替换，实例重启或某次拉取失败后，下一次会自己对齐，不依赖游标。
 *
 * 心跳的判断顺序：
 * 1. 快照不可用（还没拉到，或 Redis 异常）→ 退回原来的每次 RPOP，不能因为索引故障漏掉取消
 * 2. 快照里有这个 Job → RPOP 把队列读干。成员会一直留到 Job 结束，所以读干之后的心跳仍会再来一次，
 *    直到弹出空为止；空结果只发生在正在取消的 Job 上
 * 3. 快照里没有这个 Job → 不代表队列一定是空的。1 秒刷新只能看到索引里的成员，
 *    队列有数据但索引里没有这条成员时，刷新多少次也发现不了。这时每个 Job 仍按 [fallbackSweepMs] 读一次队列。
 *    出现这种不一致的情况有三种：
 *    - 灰度期老进程只 LPUSH 队列，不会登记索引
 *    - 快照只保留最近 [MAX_SNAPSHOT_SIZE] 条，更早的成员不在这次拉取结果里
 *    - 成员的分数是登记时间。Job 取消后超过 1 小时还没结束，过期清理会删掉索引，
 *      杀进程队列里尚未被取走的插件 ID 还在
 *
 * 可见时延：索引里有成员时，最坏约一个心跳周期；上面三种不一致，最坏是一个兜底间隔。取消的状态流转不走这里。
 */
@Component
class CancelTaskSignalService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    /**
     * 本地快照有效期。成员只有正在取消的 Job，一次全量拉取是几 KB，这里限制的是拉取频率。
     * 调小能缩短跨实例看见取消的时间，调大能再少打一些 Redis。
     */
    @Value("\${pipeline.cancelTaskSignal.snapshotCacheMs:1000}")
    private val snapshotCacheMs: Long = DEFAULT_SNAPSHOT_CACHE_MS

    /**
     * 快照里没有这个 Job 时，强制读一次队列的最小间隔。
     * 1 秒刷新解决不了“队列有数据、索引里没有”的情况。灰度期可调小，全量发布后可调大来减少空 RPOP。
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
     * 杀进程队列写入成功后登记这个 Job。
     *
     * 调用方必须先 LPUSH 再调用本方法。索引写失败只记日志，队列里的数据还在，兜底探测会读到。
     * 同时把成员放进本进程的快照，这样同一个实例上紧接着的心跳不用等快照过期。
     * 这次本地更新可能被正在进行的刷新覆盖，覆盖后最多再等一个快照周期，下一次刷新会从 Redis 把成员带回来。
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
        // 写失败也放进本地快照：本实例下一拍会去读队列，其它实例靠兜底探测
        snapshot.updateAndGet { it.copy(members = it.members + member) }
    }

    /**
     * 取出 [vmSeqId] 上待杀进程的插件 ID。
     *
     * 没有取消信号时不访问队列，返回 null。有信号时把队列读到空，或读满 [MAX_DRAIN_SIZE] 条为止；
     * 没读完的部分留在队列里，索引成员还在，下一次心跳会继续读。
     * [vmSeqId] 与写索引时的 containerId 是同一个 Job 标识，沿用心跳接口原有的入参。
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
        // 同一时刻只刷新一次。没抢到的线程继续用旧快照，晚到的取消下一拍就能看见
        if (!refreshing.compareAndSet(false, true)) {
            return current
        }
        return try {
            // ZREVRANGE 的下标是闭区间，0 到 MAX_SNAPSHOT_SIZE-1 正好是最近的 MAX_SNAPSHOT_SIZE 条。
            // 清理失败把索引撑大时，读取量停在这个上限；更老的成员靠兜底探测，队列数据不会丢
            val members = redisOperation.zrevrange(
                TaskUtils.CANCEL_SIGNAL_INDEX_KEY, 0, MAX_SNAPSHOT_SIZE - 1
            ) ?: emptySet()
            // 以这次 Redis 结果为准整份替换。刷新期间 markCancelSignal 塞进旧快照的成员可能被盖掉，
            // 该成员已经在 Redis 里，下一次刷新会回来
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
     * 按分数删掉早于 [INDEX_ENTRY_TTL_MS] 的成员。正常路径在 Job 结束时就已经删了，
     * 这里只处理 Job 没收尾、成员漏删的情况。被删掉的 Job 回到兜底探测，队列里的插件 ID 还在。
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
        /** 这次快照从 Redis 拉下来的时间，毫秒。0 表示还没拉过，下一次心跳会去拉。
         * 未过 [DEFAULT_SNAPSHOT_CACHE_MS] 就直接用这份，不再访问索引。
         */
        val time: Long,
        /** 索引里最近登记的 Job，元素是 `buildId:jobId`。心跳只拿它做成员判断，不再读 score。 */
        val members: Set<String>,
        /**
         * 这份快照能不能信。true 表示 [members] 是一次成功的拉取结果，没有这个 Job 就可以不读队列。
         * false 表示还没拉到或 Redis 异常，心跳退回每次都 RPOP，避免漏掉取消。
         */
        val available: Boolean
    )

    companion object {
        private val LOG = LoggerFactory.getLogger(CancelTaskSignalService::class.java)

        /** 本地快照有效期。过期后的下一次心跳才重新拉 Redis，所以跨实例看见取消最多等这么久。 */
        private const val DEFAULT_SNAPSHOT_CACHE_MS = 1000L

        /**
         * 快照里没有这个 Job 时，强制读一次杀进程队列的最小间隔。
         * 用来覆盖「队列有数据、索引里没有」：灰度期老进程只写队列、超出
         * 最近 [MAX_SNAPSHOT_SIZE] 条、登记超过 [INDEX_ENTRY_TTL_MS] 后索引被删掉。
         */
        private const val DEFAULT_FALLBACK_SWEEP_MS = 30000L

        /** 兜底探测记录在本地保留多久。Job 停心跳后，这条记录过期，下次心跳会重新读一次队列。 */
        private const val SWEEP_RECORD_KEEP_MINUTES = 10L

        /**
         * 同一实例两次按分数清理索引的最小间隔。清理挂在拉快照后面，不是独立定时任务；
         * 不到这个间隔就跳过，避免每秒刷新都向 Redis 发一次删除。
         */
        private const val PRUNE_INTERVAL_MS = 60000L

        /** 兜底探测记录的条数上限，大约是同时在心跳的 Job 数。超出后最老的记录被挤掉，只会多读几次队列。 */
        private const val MAX_SWEEP_CACHE_SIZE = 100000L

        /** 一次心跳最多从杀进程队列弹出的插件 ID 数。没读完的留在队列里，索引成员还在，下一次心跳继续读。 */
        private const val MAX_DRAIN_SIZE = 1000

        /** 一次快照最多保留的索引成员数，按 score 从大到小取最近登记的这些条。更早的成员靠兜底探测读队列。 */
        private const val MAX_SNAPSHOT_SIZE = 2000L

        /**
         * 索引成员按登记时间保留多久。超过后从索引删除，杀进程队列不删（队列本身保留 7 天）。
         * 取消后 Job 卡住超过这个时间，心跳改为按 [DEFAULT_FALLBACK_SWEEP_MS] 读队列，取消不会丢。
         */
        private val INDEX_ENTRY_TTL_MS = TimeUnit.HOURS.toMillis(1)
    }
}
