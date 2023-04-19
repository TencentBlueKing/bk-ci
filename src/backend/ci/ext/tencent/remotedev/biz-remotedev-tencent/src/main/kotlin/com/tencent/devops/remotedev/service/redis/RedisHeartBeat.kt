package com.tencent.devops.remotedev.service.redis

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_REMOTEDEV_INACTIVE_TIME
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class RedisHeartBeat @Autowired constructor(
    private val redisCache: RedisCacheService,
    @Qualifier("redisStringHashOperation")
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisHeartBeat::class.java)
        private const val INACTIVETIME = 1800000L
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

    fun getSleepWorkspaceHeartbeats(): List<Pair<String, String>> {
        val entries = redisOperation.hentries(heartbeatKey())

        val sleepValues = entries?.filter {
            val elapse = System.currentTimeMillis() - it.value.toLong()
            elapse > (redisCache.get(REDIS_REMOTEDEV_INACTIVE_TIME)?.toLong() ?: INACTIVETIME)
        }?.toList() ?: emptyList()

        return sleepValues
    }

    private fun heartbeatKey(): String {
        return "dispatchkubernetes:workspace_heartbeat"
    }
}
