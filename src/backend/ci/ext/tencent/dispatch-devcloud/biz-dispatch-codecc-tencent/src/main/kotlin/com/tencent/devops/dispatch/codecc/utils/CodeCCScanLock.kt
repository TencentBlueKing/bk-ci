package com.tencent.devops.dispatch.codecc.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class CodeCCScanLock(redisOperation: RedisOperation) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_CODECC_SCAN_LOCK_HOST_KEY", 60L)

    fun tryLock() =
        redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
        redisLock.unlock()
}
