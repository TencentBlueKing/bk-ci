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

import com.tencent.devops.common.api.constant.BATCH_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.FAIL_MSG
import com.tencent.devops.common.api.constant.FAIL_NUM
import com.tencent.devops.common.api.constant.KEY_START_TIME
import com.tencent.devops.common.api.constant.NAME
import com.tencent.devops.common.api.constant.SUCCESS_NUM
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.listener.EventListener
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.task.event.BatchTaskFinishEvent
import com.tencent.devops.common.task.pojo.TaskResult
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import com.tencent.devops.common.task.util.BatchTaskUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BatchTaskFinishListener @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val client: Client
) : EventListener<BatchTaskFinishEvent> {

    override fun execute(event: BatchTaskFinishEvent) {
        if (!BatchTaskUtil.isServiceMatched(event.targetService)) {
            return
        }
        val userId = event.userId
        val taskType = event.taskType
        val batchId = event.batchId
        try {
            // 获取所有结果
            val batchTaskResultKey = BatchTaskUtil.generateBatchTaskResultKey(taskType, batchId)
            val taskResults =
                redisOperation.hentries(batchTaskResultKey)?.values?.map { JsonUtil.to(it, TaskResult::class.java) }
            // 发送通知
            sendBatchTaskFinishMsg(taskResults = taskResults, taskType = taskType, batchId = batchId, userId = userId)
            // 清理Redis数据
            val keysToExpire = listOf(
                BatchTaskUtil.generateBatchTaskStartTimeKey(taskType, batchId),
                BatchTaskUtil.generateBatchTaskTotalKey(taskType, batchId),
                BatchTaskUtil.generateBatchTaskCompletedKey(taskType, batchId),
                batchTaskResultKey
            )
            keysToExpire.forEach { redisOperation.expire(it, 300) }
        } catch (ignored: Throwable) {
            val msg = "Failed to execute batch task finish bus. Details: batchId=$batchId|taskType=$taskType"
            logger.warn(msg, ignored)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.SYSTEM_ERROR,
                defaultMessage = msg
            )
        }
    }

    /**
     * 发送批量任务完成通知消息
     * @param userId 触发任务的用户ID
     * @param taskType 任务类型枚举
     * @param batchId 批量任务批次ID
     * @param taskResults 任务执行结果列表（可能为null）
     */
    private fun sendBatchTaskFinishMsg(
        userId: String,
        taskType: TaskTypeEnum,
        batchId: String,
        taskResults: List<TaskResult>?
    ) {
        // 计算任务总数、成功数和失败数
        val totalNum = taskResults?.size ?: 0
        val successNum = taskResults?.count { it.success } ?: 0
        val failNum = totalNum - successNum

        // 从Redis获取任务开始时间
        val startTime = redisOperation.get(BatchTaskUtil.generateBatchTaskStartTimeKey(taskType, batchId)) ?: ""

        // 构建消息标题参数（包含开始时间和批次ID）
        val batchTaskName = I18nUtil.getCodeLanMessage(taskType.name)
        val titleParams = mapOf(KEY_START_TIME to startTime, BATCH_ID to batchId, NAME to batchTaskName)

        // 过滤失败任务并构建错误信息
        val errorMsg = taskResults
            ?.filterNot { it.success }
            ?.takeIf { it.isNotEmpty() }
            ?.let { buildErrorMessage(it) }
            ?: ""

        // 构建消息内容参数（包含成功数、失败数和错误信息）
        val bodyParams = mapOf(
            KEY_START_TIME to startTime,
            BATCH_ID to batchId,
            NAME to batchTaskName,
            SUCCESS_NUM to successNum.toString(),
            FAIL_NUM to failNum.toString(),
            FAIL_MSG to errorMsg
        )

        // 创建通知请求对象
        val request = SendNotifyMessageTemplateRequest(
            templateCode = "BATCH_TASK_FINISH_COMMON_NOTIFY_TEMPLATE",
            receivers = mutableSetOf(userId),
            titleParams = titleParams,
            bodyParams = bodyParams,
            notifyType = mutableSetOf(NotifyType.WEWORK.name, NotifyType.EMAIL.name)
        )

        try {
            // 发送消息通知
            client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        } catch (ignored: Throwable) {
            logger.warn(
                "After the batch task is executed, the message sending fails. batchId:$batchId|taskType:$taskType",
                ignored
            )
        }
    }

    /**
     * 构建错误信息字符串
     * @param failedTasks 失败任务列表
     * @return 格式化后的错误信息字符串（最多显示前10个错误，每个错误最多500字符）
     */
    private fun buildErrorMessage(failedTasks: List<TaskResult>): String {
        if (failedTasks.isEmpty()) return ""
        // 定义错误信息格式限制参数
        val maxErrorsToShow = 10 // 最多显示前10个错误
        val maxCharsPerError = 500 // 每个错误最多显示500字符

        return failedTasks.take(maxErrorsToShow).joinToString(
            prefix = "[",
            postfix = if (failedTasks.size > maxErrorsToShow) "...]" else "]",
            separator = ", "
        ) { taskResult ->
            // 创建临时副本避免修改原始对象
            val processedResult = taskResult.copy(
                result = taskResult.result.take(maxCharsPerError).let {
                    if (it.length < taskResult.result.length) "$it..." else it
                }
            )
            JsonUtil.toJson(processedResult, false)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchTaskFinishListener::class.java)
    }
}
