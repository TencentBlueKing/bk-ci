package com.tencent.devops.dispatch.service.dispatcher.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import java.util.Date
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** issue_7748 搬用 dispatch sdk 的方法，因为sdk集成当前存在问题
 *  @see com.tencent.devops.common.dispatch.sdk.service.DispatchService
 **/
@Suppress("ALL")
@Service
class DispatchService @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    fun setRedisAuth(event: PipelineAgentStartupEvent): SecretInfo {
        val secretInfoRedisKey = secretInfoRedisKey(event.buildId)
        val redisResult = redisOperation.hget(
            key = secretInfoRedisKey,
            hashKey = secretInfoRedisMapKey(event.vmSeqId, event.executeCount ?: 1)
        )
        if (redisResult != null) {
            return JsonUtil.to(redisResult, SecretInfo::class.java)
        }
        val secretKey = ApiUtil.randomSecretKey()
        val hashId = HashUtil.encodeLongId(System.currentTimeMillis())
        logger.info("[${event.buildId}|${event.vmSeqId}] Start to build the event with ($hashId|$secretKey)")
        redisOperation.set(
            key = redisKey(hashId, secretKey),
            value = objectMapper.writeValueAsString(
                RedisBuild(
                    vmName = event.vmNames.ifBlank { "Dispatcher-sdk-${event.vmSeqId}" },
                    projectId = event.projectId,
                    pipelineId = event.pipelineId,
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId,
                    channelCode = event.channelCode,
                    zone = event.zone,
                    atoms = event.atoms,
                    executeCount = event.executeCount ?: 1
                )
            ),
            expiredInSecond = TimeUnit.DAYS.toSeconds(7)
        )

        // 一周过期时间
        redisOperation.hset(
            secretInfoRedisKey(event.buildId),
            secretInfoRedisMapKey(event.vmSeqId, event.executeCount ?: 1),
            JsonUtil.toJson(SecretInfo(hashId, secretKey), formatted = false)
        )
        val expireAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
        redisOperation.expireAt(secretInfoRedisKey, Date(expireAt))
        return SecretInfo(
            hashId = hashId,
            secretKey = secretKey
        )
    }

    private fun redisKey(hashId: String, secretKey: String) =
        "docker_build_key_${hashId}_$secretKey"

    private fun secretInfoRedisKey(buildId: String) =
        "secret_info_key_$buildId"

    private fun secretInfoRedisMapKey(vmSeqId: String, executeCount: Int) = "$vmSeqId-$executeCount"

    fun shutdown(event: PipelineAgentShutdownEvent) {
        val secretInfoKey = secretInfoRedisKey(event.buildId)

        // job结束
        finishBuild(event.vmSeqId!!, event.buildId, event.executeCount ?: 1)
        redisOperation.hdelete(secretInfoKey, secretInfoRedisMapKey(event.vmSeqId!!, event.executeCount ?: 1))
        // 当hash表为空时，redis会自动删除
    }

    private fun finishBuild(vmSeqId: String, buildId: String, executeCount: Int) {
        val result = redisOperation.hget(secretInfoRedisKey(buildId), secretInfoRedisMapKey(vmSeqId, executeCount))
        if (result != null) {
            val secretInfo = JsonUtil.to(result, SecretInfo::class.java)
            redisOperation.delete(redisKey(secretInfo.hashId, secretInfo.secretKey))
            logger.warn("$buildId|$vmSeqId finishBuild success.")
        } else {
            logger.warn("$buildId|$vmSeqId finishBuild failed, secretInfo is null.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchService::class.java)
    }
}
