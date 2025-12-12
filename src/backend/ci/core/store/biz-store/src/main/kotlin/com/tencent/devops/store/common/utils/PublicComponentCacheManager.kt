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

package com.tencent.devops.store.common.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 公共组件集合缓存管理器
 * 
 * 用于管理公共组件集合的本地缓存，减少Redis访问频率
 * 公共组件集合更新频率不高，适合缓存整个 Set
 */
object PublicComponentCacheManager {
    
    private val logger = LoggerFactory.getLogger(PublicComponentCacheManager::class.java)
    
    // 缓存过期时间（秒）
    private const val CACHE_EXPIRE_SECONDS = 60L
    
    // 公共组件集合缓存（支持多个 storeType）
    // key: storePublicFlagKey, value: Set<String> (公共组件代码列表)
    private val publicComponentSetCache: Cache<String, Set<String>> = Caffeine.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)  // 60秒过期，减少Redis压力
        .build()
    
    /**
     * 检查组件是否是公共组件
     * 
     * @param redisOperation Redis 操作对象
     * @param storeType 组件类型
     * @param storeCode 组件代码
     * @return 是否是公共组件
     */
    fun isPublicComponent(
        redisOperation: RedisOperation,
        storeType: String,
        storeCode: String
    ): Boolean {
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(storeType)
        
        // 先从本地缓存获取公共组件集合
        val publicComponentSet = publicComponentSetCache.get(storePublicFlagKey) {
            // 缓存未命中，从 Redis 加载
            try {
                redisOperation.getSetMembers(storePublicFlagKey) ?: emptySet()
            } catch (ignored: Throwable) {
                logger.warn("Failed to load public component set from Redis for key: $storePublicFlagKey", ignored)
                emptySet()  // 加载失败时返回空集合，避免影响业务
            }
        }
        return publicComponentSet.contains(storeCode)
    }
    
    /**
     * 清除公共组件集合缓存
     * 当公共组件集合更新时调用此方法，立即清除本地缓存
     * 
     * @param storeType 组件类型
     */
    fun invalidateCache(storeType: String) {
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(storeType)
        publicComponentSetCache.invalidate(storePublicFlagKey)
        logger.info("Invalidated public component cache for storeType: $storeType, key: $storePublicFlagKey")
    }
}

