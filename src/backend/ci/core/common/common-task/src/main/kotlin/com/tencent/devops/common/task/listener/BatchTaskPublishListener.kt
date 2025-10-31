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

package com.tencent.devops.common.task.listener

import com.tencent.devops.common.api.constant.ERROR
import com.tencent.devops.common.api.constant.PARAM
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.task.event.BatchTaskFinishEvent
import com.tencent.devops.common.task.event.BatchTaskPublishEvent
import com.tencent.devops.common.task.lock.BatchTaskLock
import com.tencent.devops.common.task.pojo.TaskResult
import com.tencent.devops.common.task.service.TaskExecutionService
import com.tencent.devops.common.task.service.TaskParamService
import com.tencent.devops.common.task.util.BatchTaskUtil
import com.tencent.devops.common.web.utils.I18nUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class BatchTaskPublishListener @Autowired constructor(
    private val sampleEventDispatcher: SampleEventDispatcher,
    private val redisOperation: RedisOperation
) : EventListener<BatchTaskPublishEvent> {

    /**
     * 处理批量任务发布事件
     *
     * @param event 批量任务发布事件对象，包含任务类型、批次ID、任务ID等信息
     */
    override fun execute(event: BatchTaskPublishEvent) {
        if (!BatchTaskUtil.isServiceMatched(event.targetService)) {
            return
        }
        // 执行具体任务逻辑
        val taskResult = executeTask(event)
        val taskType = event.taskType
        val batchId = event.batchId
        val taskId = event.taskId
        // 生成Redis存储key并保存任务结果
        val batchTaskResultKey = BatchTaskUtil.generateBatchTaskResultKey(taskType, batchId)
        BatchTaskLock(redisOperation, batchId).use { lock ->
            lock.lock()
            redisOperation.hset(
                key = batchTaskResultKey, hashKey = taskId, values = JsonUtil.toJson(taskResult, false)
            )
            // 设置Redis key过期时间（单位：小时转秒）
            val expiredInHour = event.expiredInHour
            redisOperation.expire(key = batchTaskResultKey, expiredInSecond = TimeUnit.HOURS.toSeconds(expiredInHour))
            // 更新完成任务计数
            val completedNum = redisOperation.increment(
                key = BatchTaskUtil.generateBatchTaskCompletedKey(taskType, batchId), incr = 1
            ) ?: 0
            // 获取总任务数
            val totalNum =
                redisOperation.get(BatchTaskUtil.generateBatchTaskTotalKey(taskType, batchId))?.toLongOrNull() ?: 0
            // 检查是否完成所有任务，若完成则触发完成事件
            if (totalNum > 0 && completedNum == totalNum) {
                sampleEventDispatcher.dispatch(
                    BatchTaskFinishEvent(
                        userId = event.userId,
                        taskType = taskType,
                        batchId = batchId,
                        targetService = event.targetService
                    )
                )
            }
        }
    }

    /**
     * 执行具体任务逻辑
     *
     * @param event 批量任务发布事件对象
     * @return TaskResult 任务执行结果对象
     */
    private fun executeTask(
        event: BatchTaskPublishEvent
    ): TaskResult {
        val taskType = event.taskType
        val batchId = event.batchId
        val taskId = event.taskId
        return try {
            // 根据任务类型获取对应的执行服务Bean
            val taskExecutionBeanName = "${taskType.name}_TASK_EXECUTION"
            val taskExecutionService =
                SpringContextUtil.getBean(TaskExecutionService::class.java, taskExecutionBeanName)
            logger.info("Starting to execute Task[$taskId] (batchId: $batchId | taskType: $taskType)")
            // 执行核心业务逻辑
            val taskResult = taskExecutionService.doBus(event)
            logger.info("Task[$taskId] execution ended. batchId: $batchId, taskType: $taskType, result: $taskResult")
            taskResult
        } catch (ignored: Throwable) {
            // 异常处理：记录日志并返回错误结果
            logger.warn("Execution of Task[$taskId] failed. batchId: $batchId, taskType: $taskType", ignored)
            val (paramText, errorText) = I18nUtil.getCodeLanMessage(PARAM) to I18nUtil.getCodeLanMessage(ERROR)
            val taskParamBeanName = "${taskType.name}_TASK_PARAM"
            val paramValue = if (SpringContextUtil.isBeanExist(taskParamBeanName)) {
                val taskParamService = SpringContextUtil.getBean(TaskParamService::class.java, taskParamBeanName)
                JsonUtil.toJson(taskParamService.getKeyParamMap(event.data), false)
            } else {
                JsonUtil.toJson(event.data, false)
            }
            TaskResult(taskId, false, "$paramText: $paramValue; $errorText: ${ignored.message}")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchTaskPublishListener::class.java)
    }
}
