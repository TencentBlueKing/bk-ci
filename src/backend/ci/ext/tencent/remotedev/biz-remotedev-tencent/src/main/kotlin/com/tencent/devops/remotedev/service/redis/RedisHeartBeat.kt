package com.tencent.devops.remotedev.service.redis

import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class RedisHeartBeat @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisHeartBeat::class.java)
    }

    fun refreshHeartbeat(
        workspaceName: String
    ) {
        logger.info("heart beat hset(${heartbeatKey()}) $workspaceName")
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
