package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class TaskIdLock(redisOperation: RedisOperation, buildId: String, taskId: String) :
    RedisLock(redisOperation = redisOperation, lockKey = "lock:build:$buildId:task:$taskId", expiredTimeInSeconds = 20)