package com.tencent.devops.process.service

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 最近使用流水线服务
 */
@Service
class PipelineRecentUseService @Autowired constructor(
    private val redisOperation: RedisOperation
) {

    fun listPipelineIds(userId: String, projectId: String, noEmpty: Boolean = true): List<String> {
        return redisOperation.zrange(
            getRedisKey(userId, projectId), 0, RECENT_USE_LIST_MAX - 1
        )?.toList() ?: (if (noEmpty) listOf("##NONE##") else emptyList())
    }

    fun record(userId: String, projectId: String, pipelineId: String) {
        try {
            val redisKey = getRedisKey(userId, projectId)
            redisOperation.zadd(redisKey, pipelineId, score())
            redisOperation.zremoveRange(redisKey, RECENT_USE_LIST_MAX, -1)
        } catch (e: Exception) {
            logger.warn("push pipeline id error", e)
        }
    }

    private fun score() = (Long.MAX_VALUE - System.currentTimeMillis()).toDouble()

    private fun getRedisKey(userId: String, projectId: String) = "$RECENT_USE_KEY:$userId:$projectId"

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRecentUseService::class.java)
        private const val RECENT_USE_KEY = "p:recent:use:pid"
        private const val RECENT_USE_LIST_MAX = 30L
    }
}
