package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpAgentResource
import com.tencent.devops.dispatch.service.ThirdPartyDispatchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAgentResourceImpl @Autowired constructor(
    private val redisOperation: RedisOperation
) : OpAgentResource {
    override fun updateGrayQueue(projectId: String, operate: String, pipelineIds: Set<String>?) {
        val redisKey = ThirdPartyDispatchService.DISPATCH_QUEUE_GRAY_PROJECT_PIPELINE
        val value = redisOperation.hget(redisKey, projectId)
        if (operate == "ADD") {
            if (value == null) {
                redisOperation.hset(redisKey, projectId, pipelineIds?.joinToString(";") ?: "")
            } else {
                val pips = value.split(";").toMutableSet()
                pips.addAll(pipelineIds ?: setOf())
                redisOperation.hset(redisKey, projectId, pips.joinToString(";"))
            }
            return
        }
        if (operate == "REMOVE") {
            if (pipelineIds.isNullOrEmpty() || value == null) {
                redisOperation.hdelete(redisKey, projectId)
            } else {
                val pips = value.split(";").toMutableSet()
                pips.removeAll(pipelineIds)
                redisOperation.hset(redisKey, projectId, pips.joinToString(";"))
            }
            return
        }
    }
}