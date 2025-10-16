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

        // 本地缓存，避免高频查询Redis。缓存5秒。
        private val projectModeCache = CacheHelper.createCache<String, RoutingMode>(
            maxSize = 5000,
            duration = 5,
            unit = TimeUnit.SECONDS
        )

        // 用于缓存全量默认模式，避免高频查询Redis。缓存5秒。
        private val defaultModeCache = CacheHelper.createCache<String, RoutingMode>(
            maxSize = 1, // 默认模式只有一个，所以最大容量为1
            duration = 5,
            unit = TimeUnit.SECONDS
        )
    }

    override fun getModeForProject(projectCode: String): RoutingMode {
        val mode = projectModeCache.get(projectCode) { loadModeFromRedis(projectCode) }!!
        logger.info("Get project mode '{}' for project '{}' from cache.", mode, projectCode)
        return mode
    }

    override fun getDefaultMode(): RoutingMode {
        // 先从本地缓存获取默认模式
        return defaultModeCache.get(DEFAULT_MODE_KEY) {
            // 如果缓存未命中或已过期，则从Redis加载
            try {
                val defaultModeName = redisOperation.get(DEFAULT_MODE_KEY)
                if (!defaultModeName.isNullOrBlank()) {
                    logger.debug("Get default mode '{}' from Redis.", defaultModeName)
                    parseMode(defaultModeName)
                } else {
                    RoutingMode.NORMAL
                }
            } catch (e: Exception) {
                // 在Redis异常时提供安全保障
                logger.error(
                    "Failed to load default permission routing strategy from Redis. " +
                        "Falling back to NORMAL mode.", e
                )
                RoutingMode.NORMAL
            }
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
            val defaultMode = getDefaultMode()
            logger.debug("Project '{}' is using default mode '{}' from getDefaultMode().", projectCode, defaultMode)
            return defaultMode
        } catch (e: Exception) {
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
