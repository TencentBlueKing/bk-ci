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
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * API 访问限制列表缓存管理器
 * 
 * 用于管理项目/流水线 API 访问限制列表的本地缓存
 * 支持在 Redis Set 更新时立即清除本地缓存，保证实时性
 * 
 * 优化点：
 * 1. 缓存 key 值，避免重复计算
 * 2. 统一缓存加载逻辑，减少代码重复
 * 3. 添加异常处理，提高健壮性
 * 4. 使用 refreshAfterWrite 实现异步刷新，提升性能
 * 5. 支持迁移中流水线状态的按需缓存（按流水线ID缓存，适应频繁变动）
 */
object ApiAccessLimitCacheManager {
    
    private val logger = LoggerFactory.getLogger(ApiAccessLimitCacheManager::class.java)
    
    // 缓存 key 值，避免重复计算
    private val PROJECT_LIMIT_KEY = BkApiUtil.getApiAccessLimitProjectsKey()
    private val PIPELINE_LIMIT_KEY = BkApiUtil.getApiAccessLimitPipelinesKey()
    
    /**
     * 项目缓存过期时间（秒）
     * 用于项目限制列表的 Set 缓存
     */
    const val PROJECT_CACHE_EXPIRE_SECONDS = 30L
    
    /**
     * 状态缓存过期时间（秒）
     * 用于单个流水线/项目状态的缓存
     * 注意：适当延长过期时间可以减少Redis查询压力，但会影响实时性
     */
    const val STATUS_CACHE_EXPIRE_SECONDS = 5L
    
    /**
     * 状态缓存最大大小
     * 用于限制单个状态缓存的最大条目数
     * 注意：应该设置得足够大，以缓存大部分查询过的流水线状态，减少Redis压力
     */
    const val STATUS_CACHE_MAX_SIZE = 50000L
    
    // 迁移中流水线状态缓存（按流水线ID缓存，支持多个 moduleCode）
    // key: "moduleCode:pipelineId", value: Boolean (是否在迁移中)
    // 使用短过期时间（5秒），因为迁移状态变动频繁
    private val migratingPipelineStatusCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .maximumSize(STATUS_CACHE_MAX_SIZE)
        .expireAfterWrite(STATUS_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build()
    
    // 项目权限限制列表缓存
    // 使用 refreshAfterWrite 实现异步刷新，提升性能
    // 当 Redis Set 更新时，通过 invalidateProjectLimitCache() 立即清除缓存
    private val projectAccessLimitSetCache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(1)
        .refreshAfterWrite(PROJECT_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)  // 30秒后异步刷新（兜底策略）
        .build()
    
    // 流水线权限限制状态缓存（按流水线ID缓存，适应频繁变动）
    // key: pipelineId, value: Boolean (是否在限制列表中)
    // 使用短过期时间（5秒），因为限制列表变动频繁
    private val pipelineAccessLimitStatusCache: Cache<String, Boolean> = Caffeine.newBuilder()
        .maximumSize(STATUS_CACHE_MAX_SIZE)
        .expireAfterWrite(STATUS_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * 构建迁移中流水线的缓存 key
     */
    private fun buildMigratingCacheKey(moduleCode: String, pipelineId: String): String {
        return "$moduleCode:$pipelineId"
    }
    
    /**
     * 从 Redis 加载限制列表
     * 
     * @param redisOperation Redis 操作对象
     * @param key Redis key
     * @return 限制列表 Set，如果加载失败返回空 Set
     */
    private fun loadFromRedis(redisOperation: RedisOperation, key: String): Set<String> {
        return try {
            redisOperation.getSetMembers(key) ?: emptySet()
        } catch (e: Exception) {
            logger.warn("Failed to load access limit set from Redis for key: $key", e)
            emptySet()  // 加载失败时返回空 Set，避免影响业务
        }
    }
    
    /**
     * 获取项目限制列表（带自动加载）
     * 
     * @param redisOperation Redis 操作对象
     * @return 项目限制列表 Set
     */
    fun getProjectLimitSet(redisOperation: RedisOperation): Set<String> {
        return try {
            projectAccessLimitSetCache.get(PROJECT_LIMIT_KEY) {
                loadFromRedis(redisOperation, PROJECT_LIMIT_KEY)
            }
        } catch (e: Exception) {
            logger.warn("Failed to get project limit set from cache, fallback to Redis", e)
            // 如果缓存获取失败，尝试直接从 Redis 加载
            loadFromRedis(redisOperation, PROJECT_LIMIT_KEY)
        }
    }
    
    /**
     * 从缓存检查状态，如果缓存未命中则从 Redis 查询
     * 
     * 优化策略：缓存所有查询过的状态（包括 true 和 false）
     * 这样可以减少Redis查询压力，特别是对于大量不在限制列表中的流水线
     * 
     * @param cache 缓存对象
     * @param cacheKey 缓存 key
     * @param redisKey Redis key
     * @param item 要检查的项
     * @param redisOperation Redis 操作对象
     * @param itemName 项名称（用于日志）
     * @return 是否在 Redis Set 中
     */
    private fun checkStatusFromCache(
        cache: Cache<String, Boolean>,
        cacheKey: String,
        redisKey: String,
        item: String,
        redisOperation: RedisOperation,
        itemName: String
    ): Boolean {
        return try {
            // 先从缓存获取（缓存所有查询过的状态，包括 true 和 false）
            cache.get(cacheKey) {
                // 缓存未命中，从 Redis 查询并缓存结果（包括 false）
                redisOperation.isMember(redisKey, item)
            }
        } catch (ignored: Throwable) {
            logger.warn("Failed to check $itemName status from cache for $itemName: $item, fallback to Redis", ignored)
            redisOperation.isMember(redisKey, item)
        }
    }
    
    /**
     * 批量检查流水线是否在限制列表中
     * 
     * @param redisOperation Redis 操作对象
     * @param pipelineIds 流水线ID列表
     * @return Map<String, Boolean> 流水线ID -> 是否在限制列表中
     */
    fun checkPipelineLimitStatus(
        redisOperation: RedisOperation,
        pipelineIds: Array<String>
    ): Map<String, Boolean> {
        return batchCheckStatus(
            cache = pipelineAccessLimitStatusCache,
            redisKey = PIPELINE_LIMIT_KEY,
            items = pipelineIds,
            redisOperation = redisOperation,
            cacheKeyMapper = { it },  // pipelineId 直接作为 cache key
            itemName = "pipeline"
        )
    }
    
    /**
     * 获取流水线限制列表（兼容方法，用于批量查询场景）
     * 
     * @deprecated 建议使用 checkPipelineLimitStatus() 方法，性能更好
     * @param redisOperation Redis 操作对象
     * @return 流水线限制列表 Set（从 Redis 加载，不缓存）
     */
    @Deprecated("建议使用 checkPipelineLimitStatus() 方法，性能更好")
    fun getPipelineLimitSet(redisOperation: RedisOperation): Set<String> {
        // 为了兼容性，直接从 Redis 加载（不缓存）
        return loadFromRedis(redisOperation, PIPELINE_LIMIT_KEY)
    }
    
    /**
     * 清除项目限制列表缓存
     * 当 Redis Set 更新时调用此方法，立即清除本地缓存
     */
    fun invalidateProjectLimitCache() {
        projectAccessLimitSetCache.invalidate(PROJECT_LIMIT_KEY)
        logger.info("Invalidated project access limit cache: $PROJECT_LIMIT_KEY")
    }
    
    /**
     * 清除单个流水线的限制状态缓存
     * 当流水线的限制状态更新时调用此方法，立即清除本地缓存
     * 
     * @param pipelineId 流水线ID
     */
    fun invalidatePipelineLimitCache(pipelineId: String) {
        pipelineAccessLimitStatusCache.invalidate(pipelineId)
        logger.info("Invalidated pipeline access limit cache for pipeline: $pipelineId")
    }
    
    /**
     * 清除多个流水线的限制状态缓存
     * 
     * @param pipelineIds 流水线ID列表
     */
    fun invalidatePipelineLimitCache(pipelineIds: List<String>) {
        pipelineIds.forEach { pipelineId ->
            pipelineAccessLimitStatusCache.invalidate(pipelineId)
        }
        logger.info("Invalidated pipeline access limit cache for ${pipelineIds.size} pipelines")
    }
    
    /**
     * 清除所有流水线限制状态缓存（用于批量更新场景）
     * 
     * 注意：此方法会清除所有流水线的缓存，建议优先使用 invalidatePipelineLimitCache(pipelineIds) 精确清除
     */
    fun invalidateAllPipelineLimitCache() {
        pipelineAccessLimitStatusCache.invalidateAll()
        logger.info("Invalidated all pipeline access limit cache")
    }
    
    /**
     * 检查单个流水线是否在迁移中（带缓存）
     * 
     * @param redisOperation Redis 操作对象
     * @param moduleCode 模块标识（如 SystemModuleEnum.PROCESS.name）
     * @param pipelineId 流水线ID
     * @return 是否在迁移中
     */
    private fun isMigratingPipeline(
        redisOperation: RedisOperation,
        moduleCode: String,
        pipelineId: String
    ): Boolean {
        val cacheKey = buildMigratingCacheKey(moduleCode, pipelineId)
        val redisKey = BkApiUtil.getMigratingPipelinesRedisKey(moduleCode)
        
        return checkStatusFromCache(
            cache = migratingPipelineStatusCache,
            cacheKey = cacheKey,
            redisKey = redisKey,
            item = pipelineId,
            redisOperation = redisOperation,
            itemName = "migrating pipeline"
        )
    }
    
    /**
     * 批量检查状态（通用方法）
     * 
     * 优化策略：缓存所有查询过的状态（包括 true 和 false）
     * 这样可以减少Redis查询压力，特别是对于大量不在限制列表中的流水线
     * 
     * @param cache 缓存对象
     * @param redisKey Redis key
     * @param items 要检查的项列表
     * @param redisOperation Redis 操作对象
     * @param cacheKeyMapper 缓存 key 映射函数（将 item 转换为 cache key）
     * @param itemName 项名称（用于日志）
     * @return Map<String, Boolean> 项 -> 是否在 Redis Set 中
     */
    private fun <T> batchCheckStatus(
        cache: Cache<String, Boolean>,
        redisKey: String,
        items: Array<T>,
        redisOperation: RedisOperation,
        cacheKeyMapper: (T) -> String,
        itemName: String
    ): Map<String, Boolean> {
        if (items.isEmpty()) {
            return emptyMap()
        }
        
        val result = mutableMapOf<String, Boolean>()
        val uncachedItems = mutableListOf<T>()
        
        // 先检查缓存（缓存所有查询过的状态，包括 true 和 false）
        items.forEach { item ->
            val cacheKey = cacheKeyMapper(item)
            val cached = cache.getIfPresent(cacheKey)
            if (cached != null) {
                // 缓存命中（可能是 true 或 false）
                result[item.toString()] = cached
            } else {
                // 缓存未命中，需要查询 Redis
                uncachedItems.add(item)
            }
        }
        
        // 批量查询未缓存的项状态
        if (uncachedItems.isNotEmpty()) {
            try {
                val batchResult = redisOperation.isMember(
                    key = redisKey,
                    items = uncachedItems.map { it.toString() }.toTypedArray()
                )
                
                // 更新结果并写入缓存（缓存所有状态，包括 false）
                uncachedItems.forEach { item ->
                    val itemStr = item.toString()
                    val status = batchResult[itemStr] ?: false
                    result[itemStr] = status
                    
                    // 缓存所有查询过的状态（包括 true 和 false），减少Redis压力
                    val cacheKey = cacheKeyMapper(item)
                    cache.put(cacheKey, status)
                }
            } catch (ignored: Throwable) {
                logger.warn(
                    "Failed to batch check $itemName status from Redis, fallback to individual queries",
                    ignored
                )
                // 批量查询失败，逐个查询
                uncachedItems.forEach { item ->
                    val itemStr = item.toString()
                    val cacheKey = cacheKeyMapper(item)
                    val status = checkStatusFromCache(
                        cache = cache,
                        cacheKey = cacheKey,
                        redisKey = redisKey,
                        item = itemStr,
                        redisOperation = redisOperation,
                        itemName = itemName
                    )
                    result[itemStr] = status
                }
            }
        }
        
        return result
    }
    
    /**
     * 批量检查流水线是否在迁移中（优化版：按需缓存单个流水线状态）
     * 
     * 优化策略：
     * 1. 不缓存整个 Set，而是按需缓存单个流水线的状态
     * 2. 使用短过期时间（5秒），快速失效
     * 3. 批量查询时，先检查缓存，未命中的再批量查询 Redis
     * 
     * @param redisOperation Redis 操作对象
     * @param moduleCode 模块标识（如 SystemModuleEnum.PROCESS.name）
     * @param pipelineIds 流水线ID列表
     * @return Map<String, Boolean> 流水线ID -> 是否在迁移中
     */
    fun checkMigratingPipelines(
        redisOperation: RedisOperation,
        moduleCode: String,
        pipelineIds: Array<String>
    ): Map<String, Boolean> {
        val redisKey = BkApiUtil.getMigratingPipelinesRedisKey(moduleCode)
        return batchCheckStatus(
            cache = migratingPipelineStatusCache,
            redisKey = redisKey,
            items = pipelineIds,
            redisOperation = redisOperation,
            cacheKeyMapper = { buildMigratingCacheKey(moduleCode, it) },
            itemName = "migrating pipeline"
        )
    }
    
    /**
     * 清除单个流水线的迁移状态缓存
     * 当流水线的迁移状态更新时调用此方法，立即清除本地缓存
     * 
     * @param moduleCode 模块标识（如 SystemModuleEnum.PROCESS.name）
     * @param pipelineId 流水线ID
     */
    fun invalidateMigratingPipelineCache(moduleCode: String, pipelineId: String) {
        val cacheKey = buildMigratingCacheKey(moduleCode, pipelineId)
        migratingPipelineStatusCache.invalidate(cacheKey)
        logger.info("Invalidated migrating pipeline cache: $cacheKey")
    }
}

