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

package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import kotlin.math.abs

/**
 * 流量控制服务
 * 基于Redis实现灰度流量控制，支持按比例切换不同的URL
 */
@Service
class TrafficControlService(
    private val redisOperation: RedisOperation
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TrafficControlService::class.java)
        
        // Redis键前缀
        private const val TRAFFIC_CONTROL_PREFIX = "devcloud:traffic:control"
        private const val GRAY_RATIO_KEY = "$TRAFFIC_CONTROL_PREFIX:gray_ratio"
        private const val WHITELIST_KEY = "$TRAFFIC_CONTROL_PREFIX:whitelist"
        private const val BLACKLIST_KEY = "$TRAFFIC_CONTROL_PREFIX:blacklist"
        
        // Default gray ratio (0-100)
        private const val DEFAULT_GRAY_RATIO = 0
        private const val MIN_GRAY_RATIO = 0
        private const val MAX_GRAY_RATIO = 100
        
        // Cache expire time in seconds
        private const val CACHE_EXPIRE_TIME = 60L
        
        // Hash calculation constants
        private const val HASH_ALGORITHM = "MD5"
        private const val HASH_BYTES_COUNT = 4
    }

    @Value("\${devCloud.traffic.control.enabled:true}")
    private val trafficControlEnabled: Boolean = true

    @Value("\${devCloud.traffic.control.cacheExpire:60}")
    private val cacheExpireTime: Long = CACHE_EXPIRE_TIME

    // 本地缓存，避免频繁访问Redis
    @Volatile
    private var cachedGrayRatio: Int? = null
    
    @Volatile
    private var cacheUpdateTime: Long = 0

    /**
     * 判断是否应该使用新URL
     * 
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @return true表示使用新URL，false表示使用旧URL
     */
    fun shouldUseNewUrl(projectId: String, pipelineId: String): Boolean {
        require(projectId.isNotBlank()) { "ProjectId cannot be blank" }
        require(pipelineId.isNotBlank()) { "PipelineId cannot be blank" }
        
        if (!trafficControlEnabled) {
            logger.debug(
                "Traffic control is disabled, using old URL for " +
                "projectId: $projectId, pipelineId: $pipelineId"
            )
            return false
        }

        try {
            // 检查白名单
            if (isInWhitelist(projectId, pipelineId)) {
                logger.debug(
                    "Project $projectId/pipeline $pipelineId is in whitelist, using new URL"
                )
                return true
            }

            // 检查黑名单
            if (isInBlacklist(projectId, pipelineId)) {
                logger.debug(
                    "Project $projectId/pipeline $pipelineId is in blacklist, using old URL"
                )
                return false
            }

            // 根据灰度比例决定
            val grayRatio = getGrayRatio()
            if (grayRatio <= MIN_GRAY_RATIO) {
                logger.debug("Gray ratio is $grayRatio%, using old URL for projectId: $projectId")
                return false
            }
            if (grayRatio >= MAX_GRAY_RATIO) {
                logger.debug("Gray ratio is $grayRatio%, using new URL for projectId: $projectId")
                return true
            }

            // 基于项目ID和流水线ID的哈希值进行流量分配
            val hashValue = calculateHash(projectId, pipelineId)
            val shouldUseNew = hashValue % 100 < grayRatio
            
            logger.debug(
                "Traffic routing decision - projectId: $projectId, " +
                "pipelineId: $pipelineId, hashValue: $hashValue, grayRatio: $grayRatio%, " +
                "useNewUrl: $shouldUseNew"
            )
            return shouldUseNew

        } catch (e: Exception) {
            logger.error(
                "Traffic control decision failed, fallback to old URL for " +
                "projectId: $projectId, pipelineId: $pipelineId, error: ${e.message}", e
            )
            return false
        }
    }

    /**
     * Gets current gray ratio from cache or Redis
     */
    fun getGrayRatio(): Int {
        try {
            // Check local cache
            if (isCacheValid()) {
                return cachedGrayRatio!!
            }

            // 从Redis获取
            val ratioStr = redisOperation.get(GRAY_RATIO_KEY)
            val ratio = ratioStr?.toIntOrNull() ?: DEFAULT_GRAY_RATIO

            // 更新本地缓存
            cachedGrayRatio = ratio
            cacheUpdateTime = System.currentTimeMillis()

            logger.debug("Retrieved gray ratio: $ratio% from Redis key: $GRAY_RATIO_KEY")
            return ratio

        } catch (e: Exception) {
            logger.error(
                "Failed to get gray ratio from Redis key: $GRAY_RATIO_KEY, " +
                "using default value: $DEFAULT_GRAY_RATIO%, error: ${e.message}", e
            )
            return DEFAULT_GRAY_RATIO
        }
    }

    /**
     * 设置灰度比例
     * 
     * @param ratio 灰度比例（0-100）
     */
    fun setGrayRatio(ratio: Int): Boolean {
        return try {
            val validRatio = ratio.coerceIn(MIN_GRAY_RATIO, MAX_GRAY_RATIO)
            if (ratio != validRatio) {
                logger.warn("Gray ratio $ratio is out of range, adjusted to $validRatio")
            }
            
            redisOperation.set(GRAY_RATIO_KEY, validRatio.toString())
            
            // Clear local cache
            clearCache()
            
            logger.info("Set gray ratio to: $validRatio% in Redis key: $GRAY_RATIO_KEY")
            true
        } catch (e: Exception) {
            logger.error("Failed to set gray ratio: $ratio to Redis key: $GRAY_RATIO_KEY, error: ${e.message}", e)
            false
        }
    }

    /**
     * 添加到白名单
     */
    fun addToWhitelist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String? = null
    ): Boolean {
        return try {
            val key = buildKey(projectId, pipelineId)
            redisOperation.addSetValue(WHITELIST_KEY, key)
            logger.info(
                "Operator $operatorUserId added key $key to whitelist, " +
                "projectId: $projectId, pipelineId: $pipelineId"
            )
            true
        } catch (e: Exception) {
            logger.error(
                "Operator $operatorUserId failed to add to whitelist, " +
                "projectId: $projectId, pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 从白名单移除
     */
    fun removeFromWhitelist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String? = null
    ): Boolean {
        return try {
            val key = buildKey(projectId, pipelineId)
            redisOperation.removeSetMember(WHITELIST_KEY, key)
            logger.info(
                "Operator $operatorUserId removed key $key from whitelist, " +
                "projectId: $projectId, pipelineId: $pipelineId"
            )
            true
        } catch (e: Exception) {
            logger.error(
                "Operator $operatorUserId failed to remove from whitelist, " +
                "projectId: $projectId, pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 添加到黑名单
     */
    fun addToBlacklist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String? = null
    ): Boolean {
        return try {
            val key = buildKey(projectId, pipelineId)
            redisOperation.addSetValue(BLACKLIST_KEY, key)
            logger.info(
                "Operator $operatorUserId added key $key to blacklist, " +
                "projectId: $projectId, pipelineId: $pipelineId"
            )
            true
        } catch (e: Exception) {
            logger.error(
                "Operator $operatorUserId failed to add to blacklist, " +
                "projectId: $projectId, pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 从黑名单移除
     */
    fun removeFromBlacklist(
        operatorUserId: String,
        projectId: String,
        pipelineId: String? = null
    ): Boolean {
        return try {
            val key = buildKey(projectId, pipelineId)
            redisOperation.removeSetMember(BLACKLIST_KEY, key)
            logger.info(
                "Operator $operatorUserId removed key $key from blacklist, " +
                "projectId: $projectId, pipelineId: $pipelineId"
            )
            true
        } catch (e: Exception) {
            logger.error(
                "Operator $operatorUserId failed to remove from blacklist, " +
                "projectId: $projectId, pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 检查是否在白名单中
     */
    private fun isInWhitelist(projectId: String, pipelineId: String): Boolean {
        return try {
            // 检查多个维度的白名单
            val keys = listOf(
                buildKey(projectId, pipelineId), // Exact match
                buildKey(projectId, null)        // Project only
            )
            
            keys.any { key ->
                redisOperation.isMember(WHITELIST_KEY, key)
            }
        } catch (e: Exception) {
            logger.error(
                "Failed to check whitelist for projectId: $projectId, " +
                "pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 检查是否在黑名单中
     */
    private fun isInBlacklist(projectId: String, pipelineId: String): Boolean {
        return try {
            // 检查多个维度的黑名单
            val keys = listOf(
                buildKey(projectId, pipelineId), // Exact match
                buildKey(projectId, null)        // Project only
            )
            
            keys.any { key ->
                redisOperation.isMember(BLACKLIST_KEY, key)
            }
        } catch (e: Exception) {
            logger.error(
                "Failed to check blacklist for projectId: $projectId, " +
                "pipelineId: $pipelineId, error: ${e.message}", e
            )
            false
        }
    }

    /**
     * 构建Redis键
     */
    private fun buildKey(projectId: String, pipelineId: String?): String {
        return when {
            pipelineId != null -> "$projectId:$pipelineId"
            else -> projectId
        }
    }

    /**
     * 计算哈希值，用于流量分配
     * 一致性：相同的输入总是产生相同的哈希值
     * 均匀分布：MD5算法确保哈希值在数值空间内均匀分布
     * 稳定性：项目的路由决策在灰度比例不变的情况下保持稳定
     * 可控性：通过调整灰度比例可以精确控制流量分配
     */
    private fun calculateHash(projectId: String, pipelineId: String): Int {
        val input = "$projectId:$pipelineId"
        val md5 = MessageDigest.getInstance(HASH_ALGORITHM)
        val hashBytes = md5.digest(input.toByteArray())
        
        // Take first 4 bytes and convert to positive integer
        var hash = 0
        for (i in 0 until HASH_BYTES_COUNT) {
            hash = (hash shl 8) or (hashBytes[i].toInt() and 0xFF)
        }
        
        return abs(hash)
    }

    /**
     * Clears local cache
     */
    private fun clearCache() {
        cachedGrayRatio = null
        cacheUpdateTime = 0
        logger.debug("Local cache cleared")
    }
    
    /**
     * Checks if local cache is valid
     */
    private fun isCacheValid(): Boolean {
        val now = System.currentTimeMillis()
        return cachedGrayRatio != null && (now - cacheUpdateTime) < cacheExpireTime * 1000
    }

    /**
     * Gets traffic control statistics
     */
    fun getTrafficStats(): Map<String, Any> {
        return try {
            mapOf(
                "enabled" to trafficControlEnabled,
                "grayRatio" to getGrayRatio(),
                "whitelistSize" to (redisOperation.getSetMembers(WHITELIST_KEY)?.size ?: 0),
                "blacklistSize" to (redisOperation.getSetMembers(BLACKLIST_KEY)?.size ?: 0),
                "cacheExpireTime" to cacheExpireTime
            )
        } catch (e: Exception) {
            logger.error("Failed to get traffic control statistics, error: ${e.message}", e)
            mapOf(
                "error" to (e.message ?: "Failed to get traffic control statistics"),
                "enabled" to trafficControlEnabled
            )
        }
    }
}