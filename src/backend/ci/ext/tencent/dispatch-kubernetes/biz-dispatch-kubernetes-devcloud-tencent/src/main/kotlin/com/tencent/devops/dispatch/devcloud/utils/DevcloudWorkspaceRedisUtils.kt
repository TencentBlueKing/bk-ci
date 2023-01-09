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
package com.tencent.devops.dispatch.devcloud.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.pojo.devcloud.TaskStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.stream.Collectors

@Component
class DevcloudWorkspaceRedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {

    /*-------------------------*/
    fun refreshTaskStatus(userId: String, taskUid: String, taskStatus: TaskStatus) {
        logger.info("User $userId hset(${taskStatusKey()}) $taskUid")
        redisOperation.hset(
            key = taskStatusKey(),
            hashKey = taskUid,
            values = JsonUtil.toJson(taskStatus)
        )
    }

    fun getTaskStatus(taskUid: String): TaskStatus? {
        val result = redisOperation.hget(taskStatusKey(), taskUid)
        return if (result != null) {
            return objectMapper.readValue(result, TaskStatus::class.java)
        } else {
            null
        }
    }

    fun deleteTask(taskUid: String) {
        logger.info("hdelete(${taskStatusKey()}) $taskUid")
        redisOperation.hdelete(taskStatusKey(), taskUid)
    }

    private fun taskStatusKey(): String {
        return "dispatchkubernetes:task_status"
    }

    /*-------------------------*/
    fun setDevcloudClusterIdHost(clusterId: String, host: String) {
        redisOperation.set(
            key = devcloudClusterIdKey(clusterId),
            value = host,
            expired = false
        )
    }

    fun getDevcloudClusterIdHost(clusterId: String): String? {
        return redisOperation.get(devcloudClusterIdKey(clusterId))
    }

    private fun devcloudClusterIdKey(clusterId: String): String {
        return "dispatchkubernetes:devcloud_cluster_id_$clusterId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DevcloudWorkspaceRedisUtils::class.java)
    }
}
