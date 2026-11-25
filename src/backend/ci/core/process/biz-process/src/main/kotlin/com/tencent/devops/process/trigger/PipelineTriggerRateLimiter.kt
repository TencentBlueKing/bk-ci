package com.tencent.devops.process.trigger

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 触发器固定窗口限流
 *
 * 同一限流对象在 windowSeconds 窗口内最多放行 maxCount 次，超出即限流。
 * Redis key 带窗口序号，窗口切换即重新计数，持续有请求时也不会被一直限制；
 * 窗口边界处最多可能放行约 2 倍 maxCount。
 */
@Component
class PipelineTriggerRateLimiter(
    private val redisOperation: RedisOperation
) {

    /**
     * @param triggerType 触发器类型，用于隔离不同触发器的计数
     * @param limitKey 限流对象，如制品仓库事件源
     * @return true 放行；false 已超限。maxCount 或 windowSeconds <= 0 表示不限制
     */
    fun tryAcquire(
        triggerType: String,
        limitKey: String,
        maxCount: Int,
        windowSeconds: Long
    ): Boolean {
        if (maxCount <= 0 || windowSeconds <= 0) {
            return true
        }
        val windowIndex = System.currentTimeMillis() / 1000 / windowSeconds
        val key = "$RATE_LIMIT_KEY_PREFIX:$triggerType:$limitKey:$windowIndex"
        return try {
            val count = redisOperation.increment(key, 1) ?: return true
            if (count == 1L) {
                redisOperation.expire(key, windowSeconds)
            }
            count <= maxCount
        } catch (ignored: Exception) {
            // 限流不可用时放行，不影响正常触发
            logger.warn("fail to acquire trigger rate limit|$key", ignored)
            true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerRateLimiter::class.java)
        private const val RATE_LIMIT_KEY_PREFIX = "process:trigger:rateLimit"
    }
}
