package com.tencent.devops.dispatch.codecc.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineContainerLock(redisOperation: RedisOperation, pipelieId: String, vmSeqId: String) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_CODECC_LOCK_CONTAINER_${pipelieId}_$vmSeqId", 60L)

    fun tryLock() =
            redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() =
            redisLock.unlock()
}
