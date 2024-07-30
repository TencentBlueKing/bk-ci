package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import kotlin.math.min

class ThirdPartyAgentQueueEnvLock(
    redisOperation: RedisOperation,
    projectId: String,
    queueKey: String,
    expiredTimeInSeconds: Long?
) : RedisLock(
    redisOperation = redisOperation,
    lockKey = "DISPATCH_REDIS_QUEUE_LOCK_ENV_${projectId}_$queueKey",
    expiredTimeInSeconds = expiredTimeInSeconds ?: 60L
) {

    fun tryLock(timeout: Long = 1000, interval: Long = 100): Boolean {
        val sleepTime = min(interval, timeout) // sleep时间不超过timeout
        val start = System.currentTimeMillis()
        var tryLock = tryLock()
        while (timeout > 0 && !tryLock && timeout > (System.currentTimeMillis() - start)) {
            Thread.sleep(sleepTime)
            tryLock = tryLock()
        }
        return tryLock
    }
}
