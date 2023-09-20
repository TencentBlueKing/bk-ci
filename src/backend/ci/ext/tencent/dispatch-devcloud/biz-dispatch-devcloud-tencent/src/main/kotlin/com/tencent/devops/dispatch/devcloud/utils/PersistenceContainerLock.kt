package com.tencent.devops.dispatch.devcloud.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PersistenceContainerLock(redisOperation: RedisOperation, containerName: String) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_DEVCLOUD_LOCK_CONTAINER_$containerName", 60L)

    fun tryLock() =
            redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
            redisLock.unlock()
}
