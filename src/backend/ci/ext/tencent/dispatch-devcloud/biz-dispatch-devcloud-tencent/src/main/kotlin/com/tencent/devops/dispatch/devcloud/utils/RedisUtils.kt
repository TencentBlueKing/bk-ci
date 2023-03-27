package com.tencent.devops.dispatch.devcloud.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    private val creatingContainerKey = "dispatch_dev_cloud_creating_containers"
    private val startingContainerKey = "dispatch_dev_cloud_starting_containers"

    /**
     * 设置容器创建信息，防止断服或者服务发布时导致的容器创建但未正常关闭
     */
    fun setCreatingContainer(containerName: String, userId: String) =
            redisOperation.addSetValue(creatingContainerKey, strcatUserAndContainerName(userId, containerName))

    fun getAndRemoveCreatingContainer(): Set<String>? {
        val result = redisOperation.getSetMembers(creatingContainerKey)
        redisOperation.delete(creatingContainerKey)
        return result
    }

    fun removeCreatingContainer(containerName: String, userId: String) =
            redisOperation.removeSetMember(creatingContainerKey, strcatUserAndContainerName(userId, containerName))

    fun setStartingContainer(containerName: String, userId: String) =
            redisOperation.addSetValue(startingContainerKey, strcatUserAndContainerName(userId, containerName))

    fun getAndRemoveStartingContainers(): Set<String>? {
        val result = redisOperation.getSetMembers(startingContainerKey)
        redisOperation.delete(startingContainerKey)
        return result
    }

    fun removeStartingContainer(containerName: String, userId: String) =
            redisOperation.removeSetMember(startingContainerKey, strcatUserAndContainerName(userId, containerName))

    /**
     * 设置网关认证信息
     */
    fun setDockerBuild(id: Long, secretKey: String, redisBuild: RedisBuild) =
        redisOperation.set(dockerBuildKey(id, secretKey), objectMapper.writeValueAsString(redisBuild), 24 * 7 * 3600)

    /**
     * shutdown时删除网关认证信息
     */
    fun deleteDockerBuild(id: Long, secretKey: String) =
        redisOperation.delete(dockerBuildKey(id, secretKey))

    /**
     * 设置startUp消息信息（保证消费顺序用）
     */
    fun setStartQueue(buildId: String, vmSeqId: String) {
        redisOperation.set(queueStartUpKey(buildId, vmSeqId), "", 24 * 3600)
    }

    fun existStartQueue(buildId: String, vmSeqId: String): Boolean {
        return redisOperation.hasKey(queueStartUpKey(buildId, vmSeqId))
    }

    fun deleteStartQueue(buildId: String, vmSeqId: String) {
        redisOperation.delete(queueStartUpKey(buildId, vmSeqId))
    }

    fun setShutdownCancelMessage(buildId: String, event: PipelineAgentShutdownEvent) {
        redisOperation.set(queueShutdownKey(buildId, ""), JsonUtil.toJson(event), 24 * 3600)
    }

    fun getShutdownCancelMessage(buildId: String): PipelineAgentShutdownEvent? {
        val shutdownMessage = redisOperation.get(queueShutdownKey(buildId, ""))
        return if (shutdownMessage != null && shutdownMessage.isNotEmpty()) {
            JsonUtil.to(shutdownMessage, PipelineAgentShutdownEvent::class.java)
        } else {
            null
        }
    }

    /**
     * 设置shutdown消息信息
     */
    fun setShutdownQueue(buildId: String, vmSeqId: String, event: PipelineAgentShutdownEvent) {
        redisOperation.set(
            queueShutdownKey(buildId, vmSeqId),
            JsonUtil.toJson(event),
            24 * 3600
        )
    }

    fun getShutdownQueue(buildId: String, vmSeqId: String): String? {
        return redisOperation.get(queueShutdownKey(buildId, vmSeqId))
    }

    fun deleteShutdownQueue(buildId: String, vmSeqId: String) {
        redisOperation.delete(queueShutdownKey(buildId, vmSeqId))
    }

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

    private fun dockerBuildKey(id: Long, secretKey: String) =
        "docker_build_key_${HashUtil.encodeLongId(id)}_$secretKey"

    private fun queueStartUpKey(buildId: String, vmSeqId: String) =
        "startup-$buildId-$vmSeqId"

    private fun queueShutdownKey(buildId: String, vmSeqId: String) =
        "shutdown-$buildId-$vmSeqId"

    private fun strcatUserAndContainerName(userId: String, containerName: String): String {
        return "$userId#$containerName"
    }

    private fun debugContainerNameKey(
        userId: String,
        pipelineId: String,
        vmSeqId: String
    ): String {
        return "dispatchdevcloud:debug:$userId-$pipelineId-$vmSeqId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisUtils::class.java)
    }
}
