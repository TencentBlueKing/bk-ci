package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineBuildStartLock(redisOperation: RedisOperation, pipelineId: String) :
    RedisLock(
        redisOperation = redisOperation,
        lockKey = "lock:pipeline:$pipelineId:build:start",
        expiredTimeInSeconds = 30
    )