package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class ThirdPartyAgentDockerDebugLock(
    redisOperation: RedisOperation,
    projectId: String,
    agentId: String
) {
    private val redisLock =
        RedisLock(redisOperation, "DISPATCH_REDIS_LOCK_AGENT_DOCKER_DEBUG_${projectId}_$agentId", 60L)

    fun tryLock() = redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
        redisLock.unlock()
}
