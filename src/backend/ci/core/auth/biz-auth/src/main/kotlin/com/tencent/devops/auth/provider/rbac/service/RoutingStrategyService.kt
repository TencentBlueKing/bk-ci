package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.util.CacheHelper
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class RoutingStrategyService(private val redisOperation: RedisOperation) : PermissionRoutingStrategy {
    companion object {
        private val logger = LoggerFactory.getLogger(RoutingStrategyService.javaClass)

        // [新] 使用 HASH 结构进行单点项目模式控制
        private const val PROJECT_MODES_KEY = "permission:delegation:project_modes"

        // [新] 使用 STRING 结构进行全量默认模式控制
        private const val DEFAULT_MODE_KEY = "permission:delegation:default_mode"

        // 本地缓存，避免高频查询Redis。缓存1分钟。
        private val projectModeCache = CacheHelper.createCache<String, RoutingMode>(
            maxSize = 5000,
            duration = 1,
            unit = TimeUnit.MINUTES
        )
    }

    override fun getModeForProject(projectCode: String): RoutingMode {
        return projectModeCache.get(projectCode) { loadModeFromRedis(projectCode) }!!
    }

    override fun getDefaultMode(): RoutingMode {
        try {
            val defaultModeName = redisOperation.get(DEFAULT_MODE_KEY)
            if (!defaultModeName.isNullOrBlank()) {
                logger.debug("get default mode from redis.", defaultModeName)
                return parseMode(defaultModeName)
            }
            return RoutingMode.NORMAL
        } catch (e: Exception) {
            // 在Redis异常时提供安全保障
            logger.error("Failed to load permission routing strategy from Redis. Falling back to NORMAL mode.", e)
            return RoutingMode.NORMAL
        }
    }

    private fun loadModeFromRedis(projectCode: String): RoutingMode {
        try {
            // 优先级 1: 检查 HASH 中是否有为该项目指定的特定模式
            val specificMode = redisOperation.hget(PROJECT_MODES_KEY, projectCode)
            if (!specificMode.isNullOrBlank()) {
                logger.debug("Project '{}' has a specific mode '{}' from Redis HASH.", projectCode, specificMode)
                return parseMode(specificMode)
            }

            // 优先级 2: 如果没有特定模式，则获取全局默认模式
            val defaultModeName = redisOperation.get(DEFAULT_MODE_KEY)
            if (!defaultModeName.isNullOrBlank()) {
                logger.debug("Project '{}' is using default mode '{}' from Redis STRING.", projectCode, defaultModeName)
                return parseMode(defaultModeName)
            }

            // 安全默认值: 如果 Redis 中什么都没配置，则默认为 NORMAL
            logger.warn(
                "No specific or default mode found in Redis for project '{}'. " +
                    "Falling back to NORMAL.", projectCode
            )
            return RoutingMode.NORMAL
        } catch (e: Exception) {
            // 在Redis异常时提供安全保障
            logger.error("Failed to load permission routing strategy from Redis. Falling back to NORMAL mode.", e)
            return RoutingMode.NORMAL
        }
    }

    private fun parseMode(modeName: String): RoutingMode {
        return try {
            RoutingMode.valueOf(modeName.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid routing mode in Redis: '{}'. Could not parse.", modeName)
            // 如果配置值非法，也应该有一个安全的回退
            RoutingMode.NORMAL
        }
    }
}
