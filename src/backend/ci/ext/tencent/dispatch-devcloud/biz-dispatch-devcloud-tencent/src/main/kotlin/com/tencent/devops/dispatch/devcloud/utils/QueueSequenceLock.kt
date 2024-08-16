package com.tencent.devops.dispatch.devcloud.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import kotlin.math.min

/**
 * 消息队列顺序消费锁
 */
class QueueSequenceLock(redisOperation: RedisOperation, buildId: String, vmSeqId: String) {

    private val redisLock = RedisLock(redisOperation, "DISPATCH_DEVCLOUD_QUEUE_SEQUENCE_LOCK__${buildId}_$vmSeqId", 60L)

    fun tryLock(timeout: Long = 10000, interval: Long = 40): Boolean {
        val sleep = min(interval, timeout) // 不允许sleep过长时间，最大1000ms
        val start = System.currentTimeMillis()
        var tryLock = redisLock.tryLock()
        while (timeout > 0 && !tryLock && timeout > (System.currentTimeMillis() - start)) {
            Thread.sleep(sleep)
            tryLock = redisLock.tryLock()
        }
        return tryLock
    }

    fun lock() = redisLock.lock()

    fun unlock() =
            redisLock.unlock()
}
