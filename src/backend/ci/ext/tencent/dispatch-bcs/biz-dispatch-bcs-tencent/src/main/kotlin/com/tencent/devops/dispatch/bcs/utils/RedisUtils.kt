package com.tencent.devops.dispatch.bcs.utils

import com.tencent.devops.common.redis.RedisOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    fun setDebugContainerName(
        userId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ) {
        redisOperation.set(
            debugContainerNameKey(userId, pipelineId, vmSeqId),
            containerName,
            3600 * 6,
            true
        )
    }

    fun getDebugContainerName(
        userId: String,
        pipelineId: String,
        vmSeqId: String
    ): String? {
        return redisOperation.get(debugContainerNameKey(userId, pipelineId, vmSeqId))
    }

    private fun debugContainerNameKey(
        userId: String,
        pipelineId: String,
        vmSeqId: String
    ): String {
        return "dispatchbcs:debug:$userId-$pipelineId-$vmSeqId"
    }
}
