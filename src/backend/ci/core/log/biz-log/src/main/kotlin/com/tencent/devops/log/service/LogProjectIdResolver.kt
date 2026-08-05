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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.dao.BuildProjectDao
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 解析 buildId → projectId 的映射，不依赖 process 服务。
 *
 * 查找优先级：调用方显式传入 → 本地 Caffeine 缓存 → Redis → DB（T_LOG_BUILD_PROJECT）。
 * 已知 projectId 时会反写缓存 + DB，供后续同 buildId 的事件复用。
 */
@Component
class LogProjectIdResolver(
    private val redisOperation: RedisOperation,
    private val buildProjectDao: BuildProjectDao
) {

    private val localCache = Caffeine.newBuilder()
        .maximumSize(LOCAL_CACHE_MAX)
        .expireAfterWrite(LOCAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
        .build<String, String>()

    private val negativeCacheExpireAt = ConcurrentHashMap<String, Long>()

    private val inflight = ConcurrentHashMap<String, Any>()

    /**
     * 解析 buildId 对应的 projectId。
     *
     * @param buildId 构建 ID
     * @param reported 调用方报告的 projectId（来自 HTTP header / 事件字段），可为 null
     * @return 已知的 projectId，或 null（表示未知）
     */
    fun resolve(buildId: String, reported: String?): String? {
        if (buildId.isBlank()) return reported?.takeIf { it.isNotBlank() }

        val known = reported?.takeIf { it.isNotBlank() }
        if (known != null) {
            saveToCache(buildId, known)
            return known
        }

        localCache.getIfPresent(buildId)?.let { return it }

        val fromRedis = try {
            redisOperation.get(redisKey(buildId))
        } catch (e: Exception) {
            logger.warn("Redis get failed for buildId={}: {}", buildId, e.message)
            null
        }
        if (!fromRedis.isNullOrBlank()) {
            localCache.put(buildId, fromRedis)
            return fromRedis
        }

        if (isNegativeCached(buildId)) return null

        return lookupFromDb(buildId)
    }

    private fun saveToCache(buildId: String, projectId: String) {
        localCache.put(buildId, projectId)
        negativeCacheExpireAt.remove(buildId)
        try {
            redisOperation.set(redisKey(buildId), projectId, REDIS_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Redis set failed for buildId={}: {}", buildId, e.message)
        }
        try {
            buildProjectDao.upsert(buildId, projectId)
        } catch (e: Exception) {
            logger.warn("DB upsert failed for buildId={}: {}", buildId, e.message)
        }
    }

    private fun lookupFromDb(buildId: String): String? {
        val lock = inflight.computeIfAbsent(buildId) { Any() }
        try {
            synchronized(lock) {
                localCache.getIfPresent(buildId)?.let { return it }
                val fromDb = buildProjectDao.getProjectId(buildId)
                if (fromDb != null) {
                    localCache.put(buildId, fromDb)
                    try {
                        redisOperation.set(redisKey(buildId), fromDb, REDIS_TTL_SECONDS)
                    } catch (e: Exception) {
                        logger.warn("Redis set failed after DB lookup for buildId={}: {}", buildId, e.message)
                    }
                    return fromDb
                }
                negativeCacheExpireAt[buildId] = System.currentTimeMillis() + NEGATIVE_CACHE_TTL_MS
                return null
            }
        } finally {
            inflight.remove(buildId)
        }
    }

    private fun isNegativeCached(buildId: String): Boolean {
        val expireAt = negativeCacheExpireAt[buildId] ?: return false
        if (System.currentTimeMillis() < expireAt) return true
        negativeCacheExpireAt.remove(buildId)
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogProjectIdResolver::class.java)
        private const val LOCAL_CACHE_MAX = 100_000L
        private const val LOCAL_CACHE_TTL_MINUTES = 30L
        private const val REDIS_TTL_SECONDS = 2 * 24 * 3600L
        private const val NEGATIVE_CACHE_TTL_MS = 10_000L

        private fun redisKey(buildId: String) = "log:build:project:id:$buildId"
    }
}
