package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class BuildIdLock(redisOperation: RedisOperation, buildId: String) :
    RedisLock(redisOperation = redisOperation, lockKey = "lock:build:$buildId:run", expiredTimeInSeconds = 30)