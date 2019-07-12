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

package com.tencent.devops.environment.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.environment.pojo.thirdPartyAgent.HeartbeatInfo
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyAgentRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentHeartbeatUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartbeatUtils::class.java)
    }

    fun saveHeartbeat(
        projectId: String,
        agentId: Long,
        newHeartbeatInfo: HeartbeatInfo
    ) {
        newHeartbeatInfo.projectId = projectId
        newHeartbeatInfo.agentId = agentId
        newHeartbeatInfo.heartbeatTime = System.currentTimeMillis()
        redisOperation.set(getHeartbeatKey(projectId, agentId), objectMapper.writeValueAsString(newHeartbeatInfo))
    }

    fun getHeartbeat(projectId: String, agentId: Long): HeartbeatInfo? {
        val build = redisOperation.get(getHeartbeatKey(projectId, agentId)) ?: return null
        try {
            return objectMapper.readValue(build, HeartbeatInfo::class.java)
        } catch (t: Throwable) {
            logger.warn("parse newHeartbeatInfo failed", t)
        }
        return null
    }

    private fun getHeartbeatKey(projectId: String, agentId: Long): String {
        return "environment.thirdparty.agent.heartbeat_${projectId}_$agentId"
    }

    fun getHeartbeatTime(record: TEnvironmentThirdpartyAgentRecord): Long? {
        var heartbeat = getHeartbeat(record.projectId, record.id)
        return if (heartbeat != null) {
            heartbeat.heartbeatTime
        } else {
            saveHeartbeat(record.projectId, record.id,
                HeartbeatInfo.dummyHeartbeat(record.projectId, record.id)
            )
            null
        }
    }
}