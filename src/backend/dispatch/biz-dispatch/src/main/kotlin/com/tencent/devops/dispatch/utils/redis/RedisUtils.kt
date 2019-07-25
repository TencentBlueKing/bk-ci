/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.utils.redis

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

    fun setRedisBuild(ip: String, redisBuild: RedisBuild) {
        redisOperation.set(ip, objectMapper.writeValueAsString(redisBuild))
    }

    fun getRedisBuild(ip: String): RedisBuild? {
        val build = redisOperation.get(ip) ?: return null
        try {
            return objectMapper.readValue(build, RedisBuild::class.java)
        } catch (ignored: Throwable) {
            logger.warn("Fail to covert the redis build to object($build)", ignored)
        }
        return null
    }

    fun deleteRedisBuild(ip: String) =
        redisOperation.delete(ip)

    fun setThirdPartyBuild(secretKey: String, redisBuild: ThirdPartyRedisBuild) {
        redisOperation.set(
            thirdPartyBuildKey(secretKey, redisBuild.agentId, redisBuild.buildId, redisBuild.vmSeqId),
            objectMapper.writeValueAsString(redisBuild)
        )
    }

    fun setDockerBuild(id: Long, secretKey: String, redisBuild: RedisBuild) =
        redisOperation.set(dockerBuildKey(id, secretKey), objectMapper.writeValueAsString(redisBuild))

    fun setDockerBuildLastHost(pipelineId: String, vmSeqId: String, hostIp: String) =
        redisOperation.set(dockerBuildLastHostKey(pipelineId, vmSeqId), hostIp)

    fun getDockerBuildLastHost(pipelineId: String, vmSeqId: String) =
        redisOperation.get(dockerBuildLastHostKey(pipelineId, vmSeqId))

    fun deleteDockerBuildLastHost(pipelineId: String, vmSeqId: String) =
        redisOperation.delete(dockerBuildLastHostKey(pipelineId, vmSeqId))

    fun deleteDockerBuild(id: Long, secretKey: String) =
        redisOperation.delete(dockerBuildKey(id, secretKey))

    fun deleteHeartBeat(buildId: String, vmSeqId: String) =
        redisOperation.delete(HeartBeatUtils.genHeartBeatKey(buildId, vmSeqId))

    fun deleteThirdPartyBuild(secretKey: String, agentId: String, buildId: String, vmSeqId: String) =
        redisOperation.delete(thirdPartyBuildKey(secretKey, agentId, buildId, vmSeqId))

    fun isThirdPartyAgentUpgrading(projectId: String, agentId: String): Boolean {
        return try {
            redisOperation.get(thirdPartyUpgradeKey(projectId, agentId)) == "true"
        } catch (ignored: Throwable) {
            false
        }
    }

    fun setThirdPartyAgentUpgrading(projectId: String, agentId: String) {
        redisOperation.set(thirdPartyUpgradeKey(projectId, agentId), "true", 60L)
    }

    fun thirdPartyAgentUpgradingDone(projectId: String, agentId: String) {
        redisOperation.delete(thirdPartyUpgradeKey(projectId, agentId))
    }

    private fun dockerBuildKey(id: Long, secretKey: String) =
        "docker_build_key_${HashUtil.encodeLongId(id)}_$secretKey"

    private fun dockerBuildLastHostKey(pipelineId: String, vmSeqId: String) =
        "dispatch_docker_build_last_host_key_${pipelineId}_$vmSeqId"

    private fun thirdPartyBuildKey(secretKey: String, agentId: String, buildId: String, vmSeqId: String) =
        "third_party_agent_${secretKey}_${agentId}_${buildId}_$vmSeqId"

    private fun thirdPartyUpgradeKey(projectId: String, agentId: String) =
        "third_party_agent_upgrade_${projectId}_$agentId"

    companion object {
        private val logger = LoggerFactory.getLogger(RedisUtils::class.java)
    }
}
