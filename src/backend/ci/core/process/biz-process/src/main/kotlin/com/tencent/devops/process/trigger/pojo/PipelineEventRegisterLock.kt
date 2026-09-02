package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineEventRegisterLock(
    redisOperation: RedisOperation,
    projectId: String,
    eventCode: String,
    eventSource: String,
    eventType: String,
    expiredTimeInSeconds: Long = 30
) : RedisLock(
    redisOperation = redisOperation,
    lockKey = "lock:pipeline:event:register:$projectId:$eventCode:$eventSource:$eventType",
    expiredTimeInSeconds = expiredTimeInSeconds
)
