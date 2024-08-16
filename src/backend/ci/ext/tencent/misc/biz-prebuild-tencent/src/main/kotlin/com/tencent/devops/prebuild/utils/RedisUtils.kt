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

package com.tencent.devops.prebuild.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.prebuild.pojo.InitPreProjectTask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RedisUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    fun setPreBuildInitTask(taskId: String, task: InitPreProjectTask) =
            redisOperation.set(preBuildInitTaskNameKey(taskId), objectMapper.writeValueAsString(task))

    fun getPreBuildInitTask(taskId: String): InitPreProjectTask? {
        val taskStr = redisOperation.get(preBuildInitTaskNameKey(taskId))
        return if (null != taskStr) {
            val task: InitPreProjectTask = objectMapper.readValue(taskStr)
            task
        } else {
            null
        }
    }

    private fun preBuildInitTaskNameKey(taskId: String) =
            "prebuild_init_task_$taskId"

    fun appendPreBuildInitTaskLogs(taskId: String, taskLogs: List<String>) {
        val logsStr = redisOperation.get(preBuildInitTaskLogsKey(taskId))
        if (null != logsStr) {
            val logs: MutableList<String> = objectMapper.readValue(logsStr)
            logs.addAll(taskLogs)
            redisOperation.set(preBuildInitTaskLogsKey(taskId), objectMapper.writeValueAsString(logs))
        } else {
            redisOperation.set(preBuildInitTaskLogsKey(taskId), objectMapper.writeValueAsString(taskLogs))
        }
    }

    fun getPreBuildInitTaskLogs(taskId: String): List<String> {
        val logsStr = redisOperation.get(preBuildInitTaskLogsKey(taskId))
        return if (null != logsStr) {
            val logs: List<String> = objectMapper.readValue(logsStr)
            logs
        } else {
            listOf()
        }
    }

    fun cleanPreBuildInitTaskLogs(taskId: String) {
        redisOperation.delete(preBuildInitTaskLogsKey(taskId))
    }

    private fun preBuildInitTaskLogsKey(taskId: String) =
            "prebuild_init_task_log_$taskId"

    companion object {
        private val logger = LoggerFactory.getLogger(RedisUtils::class.java)
    }
}
