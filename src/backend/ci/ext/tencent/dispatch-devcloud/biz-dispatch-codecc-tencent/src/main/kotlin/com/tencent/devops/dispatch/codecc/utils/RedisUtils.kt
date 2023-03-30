package com.tencent.devops.dispatch.codecc.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    fun setDockerBuild(id: Long, secretKey: String, redisBuild: RedisBuild) =
        redisOperation.set(dockerBuildKey(id, secretKey), objectMapper.writeValueAsString(redisBuild), 48 * 3600)

    fun deleteDockerBuild(id: Long, secretKey: String) =
        redisOperation.delete(dockerBuildKey(id, secretKey))

    fun setDockerBuildLastHost(pipelineId: String, vmSeqId: String, hostIp: String) =
        redisOperation.set(dockerBuildLastHostKey(pipelineId, vmSeqId), hostIp)

    fun getDockerBuildLastHost(pipelineId: String, vmSeqId: String) =
        redisOperation.get(dockerBuildLastHostKey(pipelineId, vmSeqId))

    fun deleteDockerBuildLastHost(pipelineId: String, vmSeqId: String) =
        redisOperation.delete(dockerBuildLastHostKey(pipelineId, vmSeqId))

    fun deleteHeartBeat(buildId: String, vmSeqId: String) =
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId))

    private fun dockerBuildLastHostKey(pipelineId: String, vmSeqId: String) =
        "dispatch_codecc_build_last_host_key_${pipelineId}_$vmSeqId"

    private fun dockerBuildKey(id: Long, secretKey: String) =
        "docker_build_key_${HashUtil.encodeLongId(id)}_$secretKey"

    companion object {
        private val logger = LoggerFactory.getLogger(RedisUtils::class.java)
    }
}
