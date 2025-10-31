package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.pojo.enum.RoutingMode
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 项目权限路由模式管理器
 * 专门用于管理 Redis 中的项目模式配置
 */
@Service
class ProjectModeManager(private val redisOperation: RedisOperation) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectModeManager::class.java)

        // 项目模式 HASH 键
        private const val PROJECT_MODES_KEY = "permission:delegation:project_modes"

        // 默认模式 STRING 键
        private const val DEFAULT_MODE_KEY = "permission:delegation:default_mode"
    }

    /**
     * 为指定项目设置路由模式
     * @param projectCode 项目代码
     * @param mode 路由模式
     * @return 是否设置成功
     */
    fun setProjectMode(projectCode: String, mode: RoutingMode): Boolean {
        return try {
            redisOperation.hset(PROJECT_MODES_KEY, projectCode, mode.name)
            logger.info("Successfully set project '{}' mode to '{}'", projectCode, mode.name)
            true
        } catch (e: Exception) {
            logger.error("Failed to set project '{}' mode to '{}'", projectCode, mode.name, e)
            false
        }
    }

    /**
     * 获取指定项目的路由模式
     * @param projectCode 项目代码
     * @return 项目模式，如果不存在返回 null
     */
    fun getProjectMode(projectCode: String): RoutingMode? {
        return try {
            val modeStr = redisOperation.hget(PROJECT_MODES_KEY, projectCode)
            if (!modeStr.isNullOrBlank()) {
                parseMode(modeStr)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get project '{}' mode from Redis", projectCode, e)
            null
        }
    }

    /**
     * 删除指定项目的路由模式配置
     * @param projectCode 项目代码
     * @return 是否删除成功
     */
    fun removeProjectMode(projectCode: String): Boolean {
        return try {
            redisOperation.hdelete(PROJECT_MODES_KEY, projectCode)
            logger.info("Successfully removed project '{}' mode configuration", projectCode)
            true
        } catch (e: Exception) {
            logger.error("Failed to remove project '{}' mode configuration", projectCode, e)
            false
        }
    }

    /**
     * 批量设置项目模式
     * @param projectCodes 项目ID
     * @param mode 路由模式
     */
    fun batchSetProjectModes(projectCodes: List<String>, mode: RoutingMode) {
        return try {
            val hashMap = projectCodes.associateWith { mode.name }
            redisOperation.hmset(PROJECT_MODES_KEY, hashMap)
        } catch (e: Exception) {
            logger.error("Failed to batch set project modes", e)
        }
    }

    /**
     * 设置全局默认模式
     * @param mode 默认路由模式
     * @return 是否设置成功
     */
    fun setDefaultMode(mode: RoutingMode): Boolean {
        return try {
            redisOperation.set(DEFAULT_MODE_KEY, mode.name)
            logger.info("Successfully set default mode to '{}'", mode.name)
            true
        } catch (e: Exception) {
            logger.error("Failed to set default mode to '{}'", mode.name, e)
            false
        }
    }

    /**
     * 获取全局默认模式
     * @return 默认模式，如果不存在返回 NORMAL
     */
    fun getDefaultMode(): RoutingMode {
        return try {
            val defaultModeStr = redisOperation.get(DEFAULT_MODE_KEY)
            if (!defaultModeStr.isNullOrBlank()) {
                parseMode(defaultModeStr)
            } else {
                logger.warn("No default mode found in Redis, using NORMAL as fallback")
                RoutingMode.NORMAL
            }
        } catch (e: Exception) {
            logger.error("Failed to get default mode from Redis, using NORMAL as fallback", e)
            RoutingMode.NORMAL
        }
    }

    /**
     * 清空所有项目模式配置
     * @return 是否清空成功
     */
    fun clearAllProjectModes(): Boolean {
        return try {
            redisOperation.delete(PROJECT_MODES_KEY)
            logger.info("Successfully cleared all project mode configurations")
            true
        } catch (e: Exception) {
            logger.error("Failed to clear all project mode configurations", e)
            false
        }
    }

    /**
     * 解析模式字符串
     * @param modeStr 模式字符串
     * @return 路由模式
     */
    private fun parseMode(modeStr: String): RoutingMode {
        return try {
            RoutingMode.valueOf(modeStr.trim().uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid routing mode: '{}', using NORMAL as fallback", modeStr)
            RoutingMode.NORMAL
        }
    }
}
