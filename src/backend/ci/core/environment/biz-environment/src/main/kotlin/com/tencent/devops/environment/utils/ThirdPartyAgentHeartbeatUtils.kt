/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.environment.utils

import com.tencent.devops.common.api.pojo.agent.NewHeartbeatInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ThirdPartyAgentHeartbeatUtils @Autowired constructor(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyAgentHeartbeatUtils::class.java)
        private val expiredInSecond = TimeUnit.HOURS.toSeconds(1)
        private const val MAX_TASKS = 10
    }

    fun saveNewHeartbeat(projectId: String, agentId: Long, newHeartbeatInfo: NewHeartbeatInfo) {
        // #5806 设置数量
        newHeartbeatInfo.busyTaskSize = newHeartbeatInfo.taskList?.size ?: 0

        newHeartbeatInfo.dockerBusyTaskSize = newHeartbeatInfo.dockerTaskList?.size ?: 0

        // #5806 防止被塞爆，数量过大就不支持展示
        if (newHeartbeatInfo.busyTaskSize > MAX_TASKS) {
            newHeartbeatInfo.taskList = newHeartbeatInfo.taskList?.subList(0, MAX_TASKS)
        }

        if (newHeartbeatInfo.dockerBusyTaskSize > MAX_TASKS) {
            newHeartbeatInfo.dockerTaskList = newHeartbeatInfo.dockerTaskList?.subList(0, MAX_TASKS)
        }

        newHeartbeatInfo.projectId = projectId
        newHeartbeatInfo.agentId = agentId
        newHeartbeatInfo.heartbeatTime = System.currentTimeMillis()
        redisOperation.set(
            key = getNewHeartbeatKey(projectId = projectId, agentId = agentId),
            value = JsonUtil.toJson(newHeartbeatInfo, false),
            expired = true,
            expiredInSecond = expiredInSecond
        )
    }

    fun getNewHeartbeat(projectId: String, agentId: Long): NewHeartbeatInfo? {
        return redisOperation.get(getNewHeartbeatKey(projectId, agentId))?.let { build ->
            try {
                JsonUtil.to(build, NewHeartbeatInfo::class.java)
            } catch (ignored: Throwable) {
                logger.warn("[${getNewHeartbeatKey(projectId, agentId)}] parse newHeartbeatInfo failed", ignored)
                null
            }
        }
    }

    private fun getNewHeartbeatKey(projectId: String, agentId: Long): String {
        return "environment.thirdparty.new.agent.heartbeat_${projectId}_$agentId"
    }

    fun getHeartbeatTime(id: Long, projectId: String): Long? {
        val newHeartbeat = getNewHeartbeat(projectId, id)
        val newHeartbeatTime = if (newHeartbeat != null) {
            newHeartbeat.heartbeatTime
        } else {
            saveNewHeartbeat(projectId, id, NewHeartbeatInfo.dummyHeartbeat(projectId, id))
            null
        }

        return newHeartbeatTime
    }
}
