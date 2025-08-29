package com.tencent.devops.process.trigger.pojo

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineCheckRunLock(redisOperation: RedisOperation, pipelineId: String) :
    RedisLock(
        redisOperation = redisOperation,
        lockKey = "lock:pipeline:$pipelineId:check:run",
        expiredTimeInSeconds = 60L
    ) {
    override fun decorateKey(key: String): String {
        // pipelineId在各集群唯一，key无需加上集群信息前缀来区分
        return key
    }
}
