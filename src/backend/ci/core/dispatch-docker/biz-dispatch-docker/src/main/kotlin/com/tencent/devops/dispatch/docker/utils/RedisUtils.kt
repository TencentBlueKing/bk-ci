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
package com.tencent.devops.dispatch.docker.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.utils.HeartBeatUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
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

    private fun dockerBuildKey(id: Long, secretKey: String) =
        "docker_build_key_${HashUtil.encodeLongId(id)}_$secretKey"

    private fun dockerBuildLastHostKey(pipelineId: String, vmSeqId: String) =
        "dispatch_docker_build_last_host_key_${pipelineId}_$vmSeqId"

    fun setRedisDebugMsg(pipelineId: String, vmSeqId: String, msg: String) {
        redisOperation.set("docker_debug_msg_key_${pipelineId}_$vmSeqId", msg, 3600L)
    }

    fun getRedisDebugMsg(pipelineId: String, vmSeqId: String): String? {
        return redisOperation.get("docker_debug_msg_key_${pipelineId}_$vmSeqId")
    }
}
