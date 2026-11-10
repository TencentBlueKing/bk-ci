/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.BuildProjectDao
import com.tencent.devops.log.pojo.LogBuildOwner
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 解析 buildId → (projectId, pipelineId) 的映射。
 *
 * 查找优先级：本地 Caffeine 缓存 → Redis → DB（T_LOG_BUILD_PROJECT）。
 * 只有构建开始的日志状态写入可以插入归属，已有记录不再覆盖。
 * 请求头里的项目只参与当次流量统计。没有归属时查询不做跨项目拦截。
 */
@Component
class LogProjectIdResolver(
    private val redisOperation: RedisOperation,
    private val buildProjectDao: BuildProjectDao
) {

    private val localCache = Caffeine.newBuilder()
        .maximumSize(LOCAL_CACHE_MAX)
        .expireAfterWrite(LOCAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
        .build<String, LogBuildOwner>()

    /**
     * 查不到归属的 buildId 短期不再回查 db。
     *
     * 必须是有界带过期的缓存：未知 buildId 的量级等于并发构建数，
     * 用普通 Map 记录过期时间的话，不再被访问的条目永远没有机会被清理。
     */
    private val negativeCache = Caffeine.newBuilder()
        .maximumSize(NEGATIVE_CACHE_MAX)
        .expireAfterWrite(NEGATIVE_CACHE_TTL_MS, TimeUnit.MILLISECONDS)
        .build<String, Boolean>()

    private val inflight = ConcurrentHashMap<String, Any>()

    /**
     * 上报路径只读取已有归属，用来对齐后续日志事件的项目。
     *
     * 请求头不落库。没有归属时原样返回上报值，仅供当次流量统计。
     */
    fun resolve(
        buildId: String,
        reportedProjectId: String?,
        reportedPipelineId: String? = null
    ): LogBuildOwner {
        val reported = LogBuildOwner.of(reportedProjectId, reportedPipelineId)
        if (buildId.isBlank()) {
            return reported
        }
        return lookup(buildId) ?: reported
    }

    /**
     * 构建开始时写入归属。已有项目 ID 时直接返回，不更新缓存和数据库。
     */
    fun assignIfAbsent(buildId: String, projectId: String, pipelineId: String) {
        val owner = LogBuildOwner.of(projectId, pipelineId)
        val storedProjectId = owner.projectId
        val storedPipelineId = owner.pipelineId
        if (buildId.isBlank() || storedProjectId.isNullOrBlank() || storedPipelineId.isNullOrBlank()) {
            return
        }
        if (!lookup(buildId)?.projectId.isNullOrBlank()) {
            return
        }
        val inserted = try {
            buildProjectDao.insertIfAbsent(buildId, storedProjectId, storedPipelineId)
        } catch (e: Exception) {
            logger.warn("insert build owner failed for buildId={}: {}", buildId, e.message)
            false
        }
        if (!inserted) {
            negativeCache.invalidate(buildId)
            lookupFromDb(buildId)
            return
        }
        cacheOwner(buildId, owner)
    }

    /**
     * 查询路径：只读已落库/缓存的归属，不接受调用方上报值。
     *
     * @return 已有归属；没有项目 ID 时返回 null，查询不做跨项目拦截
     */
    fun find(buildId: String): LogBuildOwner? {
        if (buildId.isBlank()) {
            return null
        }
        return lookup(buildId)?.takeIf { it.hasAny() }
    }

    private fun lookup(buildId: String): LogBuildOwner? {
        localCache.getIfPresent(buildId)?.let { return it }

        val fromRedis = try {
            redisOperation.get(redisKey(buildId))
        } catch (e: Exception) {
            logger.warn("Redis get failed for buildId={}: {}", buildId, e.message)
            null
        }
        decodeOwner(fromRedis)?.let {
            localCache.put(buildId, it)
            return it
        }

        if (negativeCache.getIfPresent(buildId) != null) {
            return null
        }

        return lookupFromDb(buildId)
    }

    private fun cacheOwner(buildId: String, owner: LogBuildOwner) {
        if (owner.projectId.isNullOrBlank()) {
            return
        }
        localCache.put(buildId, owner)
        negativeCache.invalidate(buildId)
        try {
            redisOperation.set(redisKey(buildId), encodeOwner(owner), REDIS_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Redis set failed for buildId={}: {}", buildId, e.message)
        }
    }

    private fun lookupFromDb(buildId: String): LogBuildOwner? {
        val lock = inflight.computeIfAbsent(buildId) { Any() }
        try {
            synchronized(lock) {
                localCache.getIfPresent(buildId)?.let { return it }
                val fromDb = buildProjectDao.getOwner(buildId)?.takeIf { it.hasAny() }
                if (fromDb != null) {
                    localCache.put(buildId, fromDb)
                    try {
                        redisOperation.set(redisKey(buildId), encodeOwner(fromDb), REDIS_TTL_SECONDS)
                    } catch (e: Exception) {
                        logger.warn("Redis set failed after DB lookup for buildId={}: {}", buildId, e.message)
                    }
                    return fromDb
                }
                negativeCache.put(buildId, true)
                return null
            }
        } finally {
            inflight.remove(buildId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogProjectIdResolver::class.java)
        private const val LOCAL_CACHE_MAX = 100_000L
        private const val LOCAL_CACHE_TTL_MINUTES = 30L
        private const val REDIS_TTL_SECONDS = 2 * 24 * 3600L
        private const val NEGATIVE_CACHE_MAX = 100_000L
        private const val NEGATIVE_CACHE_TTL_MS = 10_000L
        private const val OWNER_FIELD_SEPARATOR = '\t'

        private fun redisKey(buildId: String) = "log:build:owner:id:$buildId"

        /**
         * 兼容旧缓存：无分隔符时整段视为 projectId。
         */
        private fun decodeOwner(raw: String?): LogBuildOwner? {
            if (raw.isNullOrBlank()) {
                return null
            }
            val separator = raw.indexOf(OWNER_FIELD_SEPARATOR)
            return if (separator < 0) {
                LogBuildOwner.of(raw, null).takeIf { it.hasAny() }
            } else {
                LogBuildOwner.of(
                    projectId = raw.substring(0, separator),
                    pipelineId = raw.substring(separator + 1)
                ).takeIf { it.hasAny() }
            }
        }

        private fun encodeOwner(owner: LogBuildOwner): String {
            return "${owner.projectId.orEmpty()}$OWNER_FIELD_SEPARATOR${owner.pipelineId.orEmpty()}"
        }
    }
}
