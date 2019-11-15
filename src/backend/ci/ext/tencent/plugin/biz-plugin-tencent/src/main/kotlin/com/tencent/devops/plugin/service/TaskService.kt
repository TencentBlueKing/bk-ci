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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.plugin.pojo.TaskData
import com.tencent.devops.plugin.task.BaseTask
import com.tencent.devops.plugin.task.Task
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executors
import javax.annotation.PostConstruct

@Service
class TaskService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation
) {
    /**
     * 轮询定时器，判断是否有未执行的任务
     */
    private val timer = Timer()
    /**
     * 线程池，执行任务线程池
     */
    private val executor = Executors.newFixedThreadPool(10)
    private val redisKeyPrefix = "plugin_task_data_"
    private val redisKeyLockPrefix = "plugin_task_lock_"

    @PostConstruct
    fun startTimer() {
        timer.schedule(object : TimerTask() {
            override fun run() {
                val keys = redisOperation.keys("$redisKeyPrefix*")
                keys.forEach {
                    val taskDataContent = redisOperation.get(it)
                    val taskId = it.removePrefix(redisKeyPrefix)
                    val taskData = objectMapper.readValue(taskDataContent, TaskData::class.java)
                    execute(taskId, taskData)
                }
            }
        }, 30000L, 5000L)
    }

    /**
     * 创建任务并执行
     */
    fun create(taskData: TaskData) {
        val taskId = UUIDUtil.generate()
        val taskDataContent = objectMapper.writeValueAsString(taskData)

        redisOperation.set("$redisKeyPrefix$taskId", taskDataContent)
        execute(taskId, taskData)
    }

    /**
     * 执行任务
     * 1. 尝试给任务加锁
     * 2. 加锁失败，直接退出
     * 3. 加锁成功，执行任务并删除任务，释放锁
     */
    fun execute(taskId: String, taskData: TaskData) {
        executor.submit({
            val lockKey = "$redisKeyLockPrefix$taskId"
            val taskKey = "$redisKeyPrefix$taskId"
            val redisLock = RedisLock(redisOperation, lockKey, taskData.cost.toLong())
            redisLock.use {
                if (!redisLock.tryLock()) {
                    logger.info("taskRunner try lock $lockKey fail")
                    Thread.sleep(100)
                    return@use
                }

                if (redisOperation.hasKey(taskKey)) {
                    getTask(taskData).process(taskData)
                    redisOperation.delete(taskKey)
                }
                redisLock.unlock()
            }
        })
    }

    /**
     * 根据TaskData类名获取响应的Task
     */
    fun getTask(taskData: TaskData): BaseTask<TaskData> {
        val tasks = SpringContextUtil.getBeansWithAnnotation(Task::class.java)
        tasks.forEach {
            val task = it as BaseTask<TaskData>
            if (task.taskDataClass() == taskData::class.java) {
                return task
            }
        }
        throw RuntimeException("TaskData ${taskData::class.java} cannot find BaseTask.")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TaskService::class.java)
    }
}