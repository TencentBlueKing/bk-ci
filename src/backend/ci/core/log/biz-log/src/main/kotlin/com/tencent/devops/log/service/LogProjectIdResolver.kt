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
 * 解析 buildId → (projectId, pipelineId) 的映射，不依赖 process 服务。
 *
 * 查找优先级：本地 Caffeine 缓存 → Redis → DB（T_LOG_BUILD_PROJECT）。
 * 写入语义：首次写入优先；已有非空字段禁止覆盖，仅允许把空位补全。
 * 查询鉴权只走 [find]，不会把 URL 上的 projectId/pipelineId 写进映射，避免被越权请求污染归属。
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
     * 上报路径：用调用方报告的归属补全映射，并返回合并后的归属。
     *
     * 已有非空 projectId/pipelineId 不会被上报值覆盖。
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

        val known = lookup(buildId)
        if (known == null) {
            if (reported.hasAny()) {
                persist(buildId, reported)
            }
            return reported
        }

        val merged = known.fillEmpty(reported)
        if (merged != known) {
            persist(buildId, merged)
        }
        return merged
    }

    /**
     * 查询路径：只读已落库/缓存的归属，不接受调用方上报值。
     *
     * @return 已知归属；没有任何字段时返回 null（旧 Worker / 历史构建，查询侧降级为 URL-RBAC）
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

    private fun persist(buildId: String, owner: LogBuildOwner) {
        if (!owner.hasAny()) {
            return
        }
        localCache.put(buildId, owner)
        negativeCache.invalidate(buildId)
        try {
            redisOperation.set(redisKey(buildId), encodeOwner(owner), REDIS_TTL_SECONDS)
        } catch (e: Exception) {
            logger.warn("Redis set failed for buildId={}: {}", buildId, e.message)
        }
        try {
            buildProjectDao.upsert(buildId, owner.projectId, owner.pipelineId)
        } catch (e: Exception) {
            logger.warn("DB upsert failed for buildId={}: {}", buildId, e.message)
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
