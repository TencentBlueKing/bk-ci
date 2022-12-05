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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    fun setDockerBuild(id: Long, secretKey: String, redisBuild: RedisBuild) =
        redisOperation.set(
            key = dockerBuildKey(id, secretKey),
            value = objectMapper.writeValueAsString(redisBuild),
            expiredInSecond = TimeUnit.DAYS.toSeconds(7)
        )

    fun deleteDockerBuildLastHost(pipelineId: String, vmSeqId: String) {
        redisOperation.delete(dockerBuildLastHostKey(pipelineId, vmSeqId))
    }

    fun deleteDockerBuild(id: Long, secretKey: String) {
        redisOperation.delete(dockerBuildKey(id, secretKey))
    }

    // 专机集群项目管理白名单
    fun getSpecialProjectListKey(): String? {
        return redisOperation.get("dispatchdocker:special_project_list_key")
    }

    fun setSpecialProjectList(projectList: String) {
        redisOperation.set("dispatchdocker:special_project_list_key", projectList)
    }

    private fun dockerBuildKey(id: Long, secretKey: String) =
        "docker_build_key_${HashUtil.encodeLongId(id)}_$secretKey"

    private fun dockerBuildLastHostKey(pipelineId: String, vmSeqId: String) =
        "dispatch_docker_build_last_host_key_${pipelineId}_$vmSeqId"

    fun getRedisDebugMsg(pipelineId: String, vmSeqId: String): String? {
        return redisOperation.get("docker_debug_msg_key_${pipelineId}_$vmSeqId")
    }
}
