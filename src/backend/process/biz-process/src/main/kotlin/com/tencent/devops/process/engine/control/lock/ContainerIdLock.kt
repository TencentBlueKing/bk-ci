package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class ContainerIdLock(redisOperation: RedisOperation, buildId: String, containerId: String) :
    RedisLock(
        redisOperation = redisOperation,
        lockKey = "lock:build:$buildId:container:$containerId",
        expiredTimeInSeconds = 60
    )