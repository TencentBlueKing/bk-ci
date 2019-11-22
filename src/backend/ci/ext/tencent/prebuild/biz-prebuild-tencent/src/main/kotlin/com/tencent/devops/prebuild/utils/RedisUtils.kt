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
