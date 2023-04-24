package com.tencent.devops.remotedev.service.redis

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_REMOTEDEV_INACTIVE_TIME
import com.tencent.devops.remotedev.service.redis.RedisKeys.REDIS_WHITELIST_PERIOD
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.Calendar

@Component
class RedisHeartBeat @Autowired constructor(
    private val redisCache: RedisCacheService,
    @Qualifier("redisStringHashOperation")
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisHeartBeat::class.java)
        private const val INACTIVETIME = 1800000L
        private const val HEARTBEATKEY = "dispatchkubernetes:workspace_heartbeat"
    }

    fun refreshHeartbeat(
        workspaceName: String
    ) {

        logger.info("heart beat hset($HEARTBEATKEY) $workspaceName")
        redisOperation.hset(
            key = HEARTBEATKEY,
            hashKey = workspaceName,
            values = System.currentTimeMillis().toString()
        )
    }

    fun deleteWorkspaceHeartbeat(userId: String, workspaceName: String) {
        logger.info("User $userId hdelete($HEARTBEATKEY) $workspaceName")
        redisOperation.hdelete(HEARTBEATKEY, workspaceName)
    }

    fun getWorkspaceHeartbeatList(): MutableMap<String, String> {
        return redisOperation.hentries(HEARTBEATKEY) ?: mutableMapOf()
    }

    fun getSleepWorkspaceHeartbeats(): List<Pair<String, String>> {
        val entries = redisOperation.hentries(HEARTBEATKEY)

        val sleepValues = entries?.filter {
            val elapse = System.currentTimeMillis() - it.value.toLong()
            elapse > (redisCache.get(REDIS_REMOTEDEV_INACTIVE_TIME)?.toLong() ?: INACTIVETIME)
        }?.toList() ?: emptyList()

        return sleepValues
    }

    /**
     * 后台对工作空间自动刷新心跳
     */
    fun autoHeartbeat(): Boolean {
        if (checkIfInWhitelistPeriod()) {
            logger.info("start refresh all heart beat")
            val entries = redisOperation.hentries(HEARTBEATKEY)?.ifEmpty { null } ?: return false
            val now = System.currentTimeMillis().toString()
            redisOperation.hmset(HEARTBEATKEY, entries.mapValues { now })
            return true
        }
        return false
    }

    private fun checkIfInWhitelistPeriod(): Boolean {
        val whitelistPeriod = redisCache.getSetMembers(REDIS_WHITELIST_PERIOD)?.ifEmpty { null } ?: return false
        val now = Calendar.getInstance()
        val hours = now.get(Calendar.HOUR_OF_DAY)
        val minutes = now.get(Calendar.MINUTE)
        whitelistPeriod.forEach {
            val (start, end) = it.split("-")
            val (startHour, startMin) = start.split(":")
            val (endHour, endMin) = end.split(":")
            if (hours * 60 + minutes in
                startHour.toInt() * 60 + startMin.toInt() until endHour.toInt() * 60 + endMin.toInt()
            ) {
                return true
            }
        }
        return false
    }
}
