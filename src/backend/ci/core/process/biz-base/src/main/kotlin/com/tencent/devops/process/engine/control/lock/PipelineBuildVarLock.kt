package com.tencent.devops.process.engine.control.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class PipelineBuildVarLock(redisOperation: RedisOperation, buildId: String, name: String? = null) :
    RedisLock(
        redisOperation = redisOperation,
        lockKey = getLockKey(buildId, name),
        expiredTimeInSeconds = 10L
    ) {
    override fun decorateKey(key: String): String {
        // buildId在各集群唯一，key无需加上集群信息前缀来区分
        return key
    }
}

private fun getLockKey(buildId: String, name: String? = null): String {
    val defaultKey = "pipelineBuildVar:$buildId"
    return if (!name.isNullOrBlank()) {
        "$defaultKey:$name"
    } else {
        defaultKey
    }
}
