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
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 解析日志上报对应的 projectId：
 * 1. 优先使用上报方传入的可空 projectId
 * 2. 未传入时本地/Redis 缓存查找
 * 3. 仍未命中则调用 process batchServiceBasic 反查并回填缓存
 *
 * 反查侧做了两层保护，避免上万并发构建冷启动时打爆 process：
 * - singleflight：同一 buildId 并发只发起一次 process 查询，其余等待其结果
 * - 负缓存：process 查不到时短期内不再重复查询
 */
@Service
class LogProjectIdResolver(
    private val client: Client,
    private val redisOperation: RedisOperation
) {

    private val localCache = Caffeine.newBuilder()
        .maximumSize(LOCAL_CACHE_MAX_SIZE)
        .expireAfterAccess(LOCAL_CACHE_EXPIRE_HOURS, TimeUnit.HOURS)
        .build<String, String>()

    // 负缓存：process 反查不到的 buildId 短期内不再反查，抵御无效风暴
    private val negativeCache = Caffeine.newBuilder()
        .maximumSize(NEGATIVE_CACHE_MAX_SIZE)
        .expireAfterWrite(NEGATIVE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build<String, Boolean>()

    // singleflight：同一 buildId 并发反查只保留一次在途请求
    private val inFlight = ConcurrentHashMap<String, CompletableFuture<String?>>()

    fun resolve(buildId: String, reportedProjectId: String?): String? {
        if (buildId.isBlank()) {
            return reportedProjectId?.takeIf { it.isNotBlank() }
        }
        val reported = reportedProjectId?.takeIf { it.isNotBlank() }
        if (reported != null) {
            saveCache(buildId, reported)
            return reported
        }

        localCache.getIfPresent(buildId)?.let { return it }

        val fromRedis = redisOperation.get(redisKey(buildId))?.takeIf { it.isNotBlank() }
        if (fromRedis != null) {
            localCache.put(buildId, fromRedis)
            return fromRedis
        }

        if (negativeCache.getIfPresent(buildId) != null) {
            return null
        }

        val resolved = queryFromProcessSingleFlight(buildId)
        if (resolved != null) {
            saveCache(buildId, resolved)
        } else {
            negativeCache.put(buildId, true)
        }
        return resolved
    }

    private fun queryFromProcessSingleFlight(buildId: String): String? {
        // 已有在途请求则复用，避免同 buildId 并发重复打 process
        inFlight[buildId]?.let { return awaitQuietly(it) }
        val future = CompletableFuture<String?>()
        val existing = inFlight.putIfAbsent(buildId, future)
        if (existing != null) {
            return awaitQuietly(existing)
        }
        return try {
            val projectId = queryFromProcess(buildId)
            future.complete(projectId)
            projectId
        } catch (ignore: Exception) {
            // 正常情况下 queryFromProcess 已吞异常返回 null，这里兜底保证 future 一定 complete
            future.complete(null)
            null
        } finally {
            inFlight.remove(buildId, future)
        }
    }

    private fun awaitQuietly(future: CompletableFuture<String?>): String? {
        return try {
            future.get(PROCESS_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (ignore: Exception) {
            null
        }
    }

    private fun queryFromProcess(buildId: String): String? {
        return try {
            val result = client.get(ServiceBuildResource::class)
                .batchServiceBasic(setOf(buildId))
            val projectId = result.data?.get(buildId)?.projectId?.takeIf { it.isNotBlank() }
            if (projectId == null) {
                logger.warn("Cannot resolve projectId from process, buildId={}", buildId)
            }
            projectId
        } catch (ignore: Exception) {
            logger.warn("Query projectId from process failed, buildId={}", buildId, ignore)
            null
        }
    }

    private fun saveCache(buildId: String, projectId: String) {
        val cached = localCache.getIfPresent(buildId)
        if (cached == projectId) {
            return
        }
        localCache.put(buildId, projectId)
        try {
            redisOperation.set(
                redisKey(buildId),
                projectId,
                TimeUnit.DAYS.toSeconds(REDIS_CACHE_EXPIRE_DAYS)
            )
        } catch (ignore: Exception) {
            logger.warn("Save projectId redis cache failed, buildId={}", buildId, ignore)
        }
    }

    private fun redisKey(buildId: String) = "$REDIS_KEY_PREFIX$buildId"

    companion object {
        private val logger = LoggerFactory.getLogger(LogProjectIdResolver::class.java)
        private const val REDIS_KEY_PREFIX = "log:build:project:id:"
        private const val LOCAL_CACHE_MAX_SIZE = 100_000L
        private const val LOCAL_CACHE_EXPIRE_HOURS = 6L
        private const val REDIS_CACHE_EXPIRE_DAYS = 2L
        private const val NEGATIVE_CACHE_MAX_SIZE = 100_000L
        private const val NEGATIVE_CACHE_EXPIRE_SECONDS = 10L
        private const val PROCESS_WAIT_TIMEOUT_MS = 3_000L
    }
}
