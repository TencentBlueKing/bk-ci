package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class DockerHostLock(redisOperation: RedisOperation) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_REDIS_LOCK_DOCKER_HOST_KEY", 60L)

    fun tryLock() =
            redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
            redisLock.unlock()
}