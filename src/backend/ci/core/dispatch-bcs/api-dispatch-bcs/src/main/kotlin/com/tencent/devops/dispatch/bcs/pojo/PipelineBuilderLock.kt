package com.tencent.devops.dispatch.bcs.pojo

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineBuilderLock(redisOperation: RedisOperation, pipelieId: String, vmSeqId: String) {

    private val redisLock = RedisLock(
        redisOperation = redisOperation,
        lockKey = "DISPATCH_BCS_LOCK_BUILDER_${pipelieId}_$vmSeqId",
        expiredTimeInSeconds = 60L
    )

    fun tryLock() = redisLock.tryLock()

    fun lock() = redisLock.lock()

    fun unlock() = redisLock.unlock()
}
