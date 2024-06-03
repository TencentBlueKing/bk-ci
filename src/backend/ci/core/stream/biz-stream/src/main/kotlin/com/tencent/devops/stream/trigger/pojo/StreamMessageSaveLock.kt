package com.tencent.devops.stream.trigger.pojo

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class StreamMessageSaveLock(redisOperation: RedisOperation, userId: String, projectId: String, messageId: String) :
    RedisLock(
        redisOperation = redisOperation,
        lockKey = "lock:stream:message:userId:$userId:projectId:$projectId:messageId:$messageId",
        expiredTimeInSeconds = 60
    )
