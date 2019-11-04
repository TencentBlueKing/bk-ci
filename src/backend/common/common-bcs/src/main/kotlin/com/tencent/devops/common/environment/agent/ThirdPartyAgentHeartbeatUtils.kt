/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.common.environment.agent

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory

class ThirdPartyAgentHeartbeatUtils constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartbeatUtils::class.java)
    }

    fun saveNewHeartbeat(
        projectId: String,
        agentId: Long,
        newHeartbeatInfo: NewHeartbeatInfo
    ) {
        newHeartbeatInfo.projectId = projectId
        newHeartbeatInfo.agentId = agentId
        newHeartbeatInfo.heartbeatTime = System.currentTimeMillis()
        redisOperation.set(getNewHeartbeatKey(projectId, agentId), objectMapper.writeValueAsString(newHeartbeatInfo))
    }

    fun getNewHeartbeat(projectId: String, agentId: Long): NewHeartbeatInfo? {
        val build = redisOperation.get(getNewHeartbeatKey(projectId, agentId)) ?: return null
        try {
            return objectMapper.readValue(build, NewHeartbeatInfo::class.java)
        } catch (t: Throwable) {
            logger.warn("parse newHeartbeatInfo failed", t)
        }
        return null
    }

    private fun getNewHeartbeatKey(projectId: String, agentId: Long): String {
        return "environment.thirdparty.new.agent.heartbeat_${projectId}_$agentId"
    }

    fun heartbeat(
        projectId: String,
        agentId: String
    ) {
        redisOperation.set(getHeartbeatKey(projectId, agentId), System.currentTimeMillis().toString())
    }

    fun getHeartbeat(
        projectId: String,
        agentId: String
    ): Long? {
        return redisOperation.get(getHeartbeatKey(projectId, agentId))?.toLong()
    }

    private fun getHeartbeatKey(projectId: String, agentId: String) =
        "third-party-agent-heartbeat-$projectId-$agentId"

    fun getHeartbeatTime(id: Long, projectId: String): Long? {
        val agentId = HashUtil.encodeLongId(id)

        val oldHeartbeatTime = getHeartbeat(projectId, agentId)
        if (oldHeartbeatTime == null) {
            heartbeat(projectId, agentId)
        }

        val newHeartbeat = getNewHeartbeat(projectId, id)
        val newHeartbeatTime = if (newHeartbeat != null) {
            newHeartbeat.heartbeatTime
        } else {
            saveNewHeartbeat(projectId, id,
                NewHeartbeatInfo.dummyHeartbeat(projectId, id)
            )
            null
        }

        return if (oldHeartbeatTime != null && newHeartbeatTime != null) {
            Math.max(oldHeartbeatTime, newHeartbeatTime)
        } else {
            newHeartbeatTime ?: oldHeartbeatTime
        }
    }
}