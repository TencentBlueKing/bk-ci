package com.tencent.devops.common.misc

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

class ThirdPartyAgentHeartbeatUtils constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartbeatUtils::class.java)
    }

    fun saveNewHeartbeat(
        projectId: String,
        agentId: Long,
        newHeartbeatInfo: NewHeartbeatInfo
    ) {
        newHeartbeatInfo.projectId = projectId
        newHeartbeatInfo.agentId = agentId
        newHeartbeatInfo.heartbeatTime = System.currentTimeMillis()
        redisOperation.set(getNewHeartbeatKey(projectId, agentId), objectMapper.writeValueAsString(newHeartbeatInfo))
    }

    fun getNewHeartbeat(projectId: String, agentId: Long): NewHeartbeatInfo? {
        val build = redisOperation.get(getNewHeartbeatKey(projectId, agentId)) ?: return null
        try {
            return objectMapper.readValue(build, NewHeartbeatInfo::class.java)
        } catch (t: Throwable) {
            logger.warn("parse newHeartbeatInfo failed", t)
        }
        return null
    }

    private fun getNewHeartbeatKey(projectId: String, agentId: Long): String {
        return "environment.thirdparty.new.agent.heartbeat_${projectId}_$agentId"
    }

    fun heartbeat(
        projectId: String,
        agentId: String
    ) {
        redisOperation.set(getHeartbeatKey(projectId, agentId), System.currentTimeMillis().toString())
    }

    fun getHeartbeat(
        projectId: String,
        agentId: String
    ): Long? {
        return redisOperation.get(getHeartbeatKey(projectId, agentId))?.toLong()
    }

    private fun getHeartbeatKey(projectId: String, agentId: String) =
        "third-party-agent-heartbeat-$projectId-$agentId"

    fun getHeartbeatTime(id: Long, projectId: String): Long? {
        val agentId = HashUtil.encodeLongId(id)

        val oldHeartbeatTime = getHeartbeat(projectId, agentId)
        if (oldHeartbeatTime == null) {
            heartbeat(projectId, agentId)
        }

        val newHeartbeat = getNewHeartbeat(projectId, id)
        val newHeartbeatTime = if (newHeartbeat != null) {
            newHeartbeat.heartbeatTime
        } else {
            saveNewHeartbeat(projectId, id,
                NewHeartbeatInfo.dummyHeartbeat(projectId, id)
            )
            null
        }

        return if (oldHeartbeatTime != null && newHeartbeatTime != null) {
            Math.max(oldHeartbeatTime, newHeartbeatTime)
        } else {
            newHeartbeatTime ?: oldHeartbeatTime
        }
    }
}