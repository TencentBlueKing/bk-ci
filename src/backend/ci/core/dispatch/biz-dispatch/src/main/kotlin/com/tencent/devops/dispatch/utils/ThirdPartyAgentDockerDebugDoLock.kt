package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class ThirdPartyAgentDockerDebugDoLock(
    redisOperation: RedisOperation,
    buildId: String,
    vmSeqId: String,
    userId: String
) {
    private val redisLock =
        RedisLock(redisOperation,
            "DISPATCH_REDIS_LOCK_AGENT_DO_DOCKER_DEBUG_${buildId}_${vmSeqId}_$userId",
            60L)

    fun tryLock() = redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
        redisLock.unlock()
}
