package com.tencent.devops.environment.utils

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation

class ThirdAgentActionAddLock(
    redisOperation: RedisOperation,
    projectId: String,
    agentId: Long
) : RedisLock(
    redisOperation,
    "environment:thirdparty:agent.$projectId.$agentId.action.lock",
    expiredTimeInSeconds = 60
)