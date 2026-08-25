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

package com.tencent.devops.log.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.IndexDao
import com.tencent.devops.log.metrics.LogMetrics
import com.tencent.devops.log.util.IndexNameUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.concurrent.TimeUnit

@Suppress("NestedBlockDepth")
@Service
class IndexService @Autowired constructor(
    private val dslContext: DSLContext,
    private val indexDao: IndexDao,
    private val redisOperation: RedisOperation,
    private val logMetrics: ObjectProvider<LogMetrics>
) {

    companion object {
        private val logger = LoggerFactory.getLogger(IndexService::class.java)
        private const val LOG_INDEX_LOCK = "log:build:enable:lock:key"
        private const val LOG_LINE_NUM = "log:build:line:num:"
        private const val LOG_LINE_NUM_UNRELIABLE = "log:build:line:num:unreliable:"
        private const val INDEX_CACHE_MAX_SIZE = 100000L
        private const val INDEX_CACHE_EXPIRE_MINUTES = 30L
        private const val INDEX_LOCK_EXPIRE_SECONDS = 10L

        /** 建索引时写入 db/redis 的行号初始值，见 [IndexDao.create] */
        private const val INIT_LINE_NUM = 1L

        /** 行号缓存的存活时长，需覆盖单次构建的最长生命周期 */
        private val LINE_NUM_TTL_SECONDS = TimeUnit.DAYS.toSeconds(2)

        /**
         * 构建结束后行号缓存的保留时长。
         *
         * #13327 构建结束事件走 status 队列，日志走 origin 队列，两者积压程度不同：
         * 结束事件先被消费时，origin 里往往还有大量未落库的日志。此时若立即删除行号缓存，
         * 后续日志只能回退 db 基线重新分配行号，与已写入的日志撞上同一个确定性 _id 造成覆盖。
         * 因此结束后仅缩短 TTL 留出消费宽限期，不再直接删除。
         */
        private val LINE_NUM_GRACE_SECONDS = TimeUnit.HOURS.toSeconds(6)

        private const val UNRELIABLE_CACHE_MAX_SIZE = 10000L
        private const val UNRELIABLE_CACHE_EXPIRE_SECONDS = 30L
        private const val ALLOCATED_CACHE_MAX_SIZE = 100000L
        private const val ALLOCATED_CACHE_EXPIRE_MINUTES = 120L

        fun getLineNumRedisKey(buildId: String) = LOG_LINE_NUM + buildId

        private fun getLineNumUnreliableKey(buildId: String) = LOG_LINE_NUM_UNRELIABLE + buildId
    }

    private val indexCache = Caffeine.newBuilder()
        .maximumSize(INDEX_CACHE_MAX_SIZE)
        .expireAfterAccess(INDEX_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String/*BuildId*/, String/*IndexName*/> { buildId ->
            dslContext.transactionResult { configuration ->
                val context = DSL.using(configuration)
                var indexName = indexDao.getIndexName(context, buildId)
                if (indexName.isNullOrBlank()) {
                    val redisLock = RedisLock(
                        redisOperation = redisOperation,
                        lockKey = "$LOG_INDEX_LOCK:$buildId",
                        expiredTimeInSeconds = INDEX_LOCK_EXPIRE_SECONDS
                    )
                    redisLock.lock()
                    try {
                        indexName = indexDao.getIndexName(context, buildId)
                        if (indexName.isNullOrBlank()) {
                            logger.info("[$buildId] Add the build record")
                            indexName = saveIndex(buildId)
                        }
                    } finally {
                        redisLock.unlock()
                    }
                }
                indexName!!
            }
        }

    /**
     * 行号不可信的构建标记，避免每批日志都回查 Redis。
     * 缓存 30 秒，跨 Pod 的标记通过 Redis 传播。
     */
    private val lineNumUnreliableCache = Caffeine.newBuilder()
        .maximumSize(UNRELIABLE_CACHE_MAX_SIZE)
        .expireAfterWrite(UNRELIABLE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build<String/*BuildId*/, Boolean/*Unreliable*/>()

    /**
     * 本 Pod 为各构建分配到的行号水位，用于在行号缓存丢失时续上，并识别 db 基线滞后。
     */
    private val allocatedLineNumCache = Caffeine.newBuilder()
        .maximumSize(ALLOCATED_CACHE_MAX_SIZE)
        .expireAfterAccess(ALLOCATED_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build<String/*BuildId*/, Long/*Allocated*/>()

    private fun saveIndex(buildId: String): String {
        val indexName = IndexNameUtils.getIndexName()
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            indexDao.create(context, buildId, indexName, true)
            // 建索引晚于首批日志的行号分配（addLineNo 先于 prepareIndex），
            // 这里必须用 setIfAbsent，否则会把已经自增的行号重置回初始值
            redisOperation.setIfAbsent(
                getLineNumRedisKey(buildId), INIT_LINE_NUM.toString(), LINE_NUM_TTL_SECONDS
            )
        }
        logger.info("[$buildId|$indexName] Create new index in db and cache")
        return indexName
    }

    fun getIndexName(buildId: String): String {
        val index = indexCache.get(buildId)
        if (index.isNullOrBlank()) {
            throw OperationException("Fail to get the index of build $buildId")
        }
        return index
    }

    fun getAndAddLineNum(buildId: String, size: Int): Long? {
        val redisKey = getLineNumRedisKey(buildId)
        // 缓存命中则直接进行自增，缓存未命中则从db中取值，自增后再刷新缓存
        val cached = redisOperation.get(redisKey)?.toLongOrNull()
        if (cached == null) {
            logger.warn("[$buildId|$size] Fail to get and add the line num, get from db")
            val lastLineNum = indexDao.getBuild(dslContext, buildId)?.lastLineNum ?: run {
                logger.warn("[$buildId|$size] The build is not exist in db")
                0L
            }
            logger.warn("[$buildId|$size] Got from db, lastLineNum: $lastLineNum")
            val baseline = restoreBaseline(buildId, lastLineNum)
            // 如果设置失败说明已被另一个并发任务写入，继续执行行号自增
            val success = redisOperation.setIfAbsent(
                redisKey,
                (baseline + size.toLong()).toString(),
                LINE_NUM_TTL_SECONDS
            )
            if (success) {
                markAllocated(buildId, baseline + size.toLong())
                return baseline
            }
        }
        // 自增并刷新过期时间
        val lineNum = redisOperation.increment(redisKey, size.toLong()) ?: return null
        redisOperation.expire(redisKey, LINE_NUM_TTL_SECONDS)
        markAllocated(buildId, lineNum)
        return lineNum - size
    }

    /**
     * 行号缓存丢失后确定重新分配的基线。
     *
     * db 里的值只在构建结束时刷新，缓存中途丢失（过期/驱逐/主从切换）时它会明显滞后。
     * 本 Pod 记得自己分配到哪里，优先用这个水位续上，行号才能保持单调；
     * 一旦确认发生滞后，还要标记行号不可信 —— 并发回退的其它线程可能已经用滞后基线
     * 写入过日志，此后继续使用确定性 _id 就有覆盖历史日志的风险。
     */
    private fun restoreBaseline(buildId: String, lastLineNum: Long): Long {
        val allocated = allocatedLineNumCache.getIfPresent(buildId) ?: return lastLineNum
        if (allocated <= lastLineNum) return lastLineNum
        markLineNumUnreliable(buildId, lastLineNum, allocated)
        return allocated
    }

    private fun markAllocated(buildId: String, lineNum: Long) {
        allocatedLineNumCache.asMap().merge(buildId, lineNum) { old, new -> maxOf(old, new) }
    }

    /**
     * 行号高水位是否可信。
     *
     * 返回 false 表示该构建的行号可能与已写入 ES 的日志重复，写入侧必须放弃
     * `(buildId, executeCount, lineNo)` 确定性 _id，改用 ES 自动生成 _id ——
     * 宁可出现重复日志，也不能覆盖掉已经落库的历史日志。
     */
    fun isLineNumReliable(buildId: String): Boolean {
        lineNumUnreliableCache.getIfPresent(buildId)?.let { return !it }
        val unreliable = try {
            redisOperation.get(getLineNumUnreliableKey(buildId)) != null
        } catch (ignore: Exception) {
            logger.warn("[$buildId] Fail to check whether the line num is reliable", ignore)
            false
        }
        lineNumUnreliableCache.put(buildId, unreliable)
        return !unreliable
    }

    private fun markLineNumUnreliable(buildId: String, lastLineNum: Long, allocated: Long) {
        logger.warn(
            "[$buildId] Line num rollback detected, db lastLineNum=$lastLineNum but allocated=$allocated, " +
                "fallback to auto-generated doc id to avoid overwriting the existing logs"
        )
        lineNumUnreliableCache.put(buildId, true)
        try {
            redisOperation.set(getLineNumUnreliableKey(buildId), "1", LINE_NUM_TTL_SECONDS)
        } catch (ignore: Exception) {
            logger.warn("[$buildId] Fail to mark the line num as unreliable", ignore)
        }
        logMetrics.getIfAvailable()?.recordLineNumWatermarkLost()
    }

    fun getBuildIndexName(buildId: String): String? {
        return indexDao.getBuild(dslContext, buildId)?.indexName
    }

    fun getLastLineNum(buildId: String): Long {
        return redisOperation.get(getLineNumRedisKey(buildId))?.toLongOrNull()
            ?: indexDao.getBuild(dslContext, buildId)?.lastLineNum ?: 0
    }

    fun flushLineNum2DB(buildId: String) {
        val redisKey = getLineNumRedisKey(buildId)
        val lineNum = redisOperation.get(redisKey)
        if (lineNum.isNullOrBlank()) {
            // 行号缓存已丢失，db 里保留的可能仍是滞后值。这里无从恢复高水位，
            // 只能交由 getAndAddLineNum 用本地水位续上并标记回退
            logger.warn("[$buildId] Fail to get lineNum from redis")
            logMetrics.getIfAvailable()?.recordLineNumFlushMissed()
            return
        }
        val latestLineNum = lineNum.toLongOrNull() ?: run {
            logger.warn("[$buildId|$lineNum] Fail to convert line num to long")
            return
        }
        // 只增不减，返回 0 行既可能是记录不存在，也可能是 db 里的值已经更大（重复/乱序的结束事件）
        indexDao.updateLastLineNum(dslContext, buildId, latestLineNum)
        // 构建虽已结束，但 origin 队列里可能还有该构建的日志未消费完，
        // 删除行号缓存会让这部分日志回退 db 基线重新分配行号并覆盖历史日志，
        // 因此只缩短存活时间，留出消费宽限期
        redisOperation.expire(redisKey, LINE_NUM_GRACE_SECONDS)
    }
}
