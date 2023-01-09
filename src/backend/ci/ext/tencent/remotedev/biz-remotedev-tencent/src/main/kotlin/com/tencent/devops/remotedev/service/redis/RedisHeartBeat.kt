package com.tencent.devops.remotedev.service.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.stream.Collectors

class RedisHeartBeat @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisHeartBeat::class.java)
    }

    fun refreshHeartbeat(
        userId: String,
        workspaceName: String
    ) {
        logger.info("User $userId hset(${heartbeatKey()}) $workspaceName")
        redisOperation.hset(
            key = heartbeatKey(),
            hashKey = workspaceName,
            values = System.currentTimeMillis().toString()
        )
    }

    fun deleteWorkspaceHeartbeat(userId: String, workspaceName: String) {
        logger.info("User $userId hdelete(${heartbeatKey()}) $workspaceName")
        redisOperation.hdelete(heartbeatKey(), workspaceName)
    }

    fun getWorkspaceHeartbeatList(): MutableMap<String, String> {
        return redisOperation.hentries(heartbeatKey()) ?: mutableMapOf()
    }

    fun getSleepWorkspaceHeartbeats(): List<String> {
        val values = redisOperation.hvalues(heartbeatKey())

        val sleepValues = values?.stream()?.filter {
            val elapse = System.currentTimeMillis() - it.toLong()
            elapse > 1800000
        }?.collect(Collectors.toList()) ?: emptyList()

        return sleepValues
    }

    private fun heartbeatKey(): String {
        return "dispatchkubernetes:workspace_heartbeat"
    }
}
