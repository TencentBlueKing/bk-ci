package com.tencent.devops.dispatch.macos.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.dispatch.sdk.listener.DispatcherContext
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.macos.pojo.MacRedisBuild
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MacosVMRedisService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private const val REDIS_PRE = "dispatcher:devops_macos_"
        private const val REDIS_KEY_EXPIRE_IN_SECOND = 36000L
    }

    fun saveRedisBuild(dispatchMessage: DispatchMessage, vmIp: String) {
        val macOSEvn = dispatchMessage.dispatchMessage.split(":")
        val pair = when (macOSEvn.size) {
            0 -> Pair(null, null)
            1 -> Pair(macOSEvn[0], null)
            else -> Pair(macOSEvn[0], macOSEvn[1])
        }
        val event = DispatcherContext.getEvent()
        val atoms = event?.atoms ?: mapOf()
        val build = MacRedisBuild(
            dispatchMessage.id,
            dispatchMessage.secretKey,
            dispatchMessage.gateway,
            dispatchMessage.projectId,
            dispatchMessage.pipelineId,
            dispatchMessage.buildId,
            dispatchMessage.vmSeqId,
            pair.first ?: "",
            pair.second ?: "",
            atoms

        )
        val key = REDIS_PRE + vmIp
        redisOperation.set(key, objectMapper.writeValueAsString(build), REDIS_KEY_EXPIRE_IN_SECOND)
    }

    fun deleteRedisBuild(vmIp: String) {
        redisOperation.delete(REDIS_PRE + vmIp)
    }
}
