package com.tencent.devops.dispatch.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class VMLock(
    redisOperation: RedisOperation,
    vmIp: String
) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_REDIS_LOCK_VM_$vmIp", 60L)

    fun tryLock() =
            redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
            redisLock.unlock()
}