package com.tencent.devops.common.task.util

import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.task.pojo.TaskTypeEnum
import org.slf4j.LoggerFactory

object BatchTaskUtil {

    private val logger = LoggerFactory.getLogger(BatchTaskUtil::class.java)

    private fun generateBatchTaskKeyPrefix(taskType: TaskTypeEnum, batchId: String) =
        "batchTask:$taskType:$batchId"

    fun generateBatchTaskStartTimeKey(taskType: TaskTypeEnum, batchId: String) =
        "${generateBatchTaskKeyPrefix(taskType, batchId)}:startTime"

    fun generateBatchTaskTotalKey(taskType: TaskTypeEnum, batchId: String) =
        "${generateBatchTaskKeyPrefix(taskType, batchId)}:total"

    fun generateBatchTaskCompletedKey(taskType: TaskTypeEnum, batchId: String) =
        "${generateBatchTaskKeyPrefix(taskType, batchId)}:completed"

    fun generateBatchTaskResultKey(taskType: TaskTypeEnum, batchId: String) =
        "${generateBatchTaskKeyPrefix(taskType, batchId)}:results"

    /**
     * 检查当前服务是否匹配目标服务
     *
     * @param targetService 需要匹配的目标服务名称
     * @return Boolean 返回true表示服务匹配，false表示不匹配
     */
    fun isServiceMatched(targetService: String?): Boolean {
        if (targetService.isNullOrBlank()) return false

        val currentService = BkServiceUtil.getApplicationName() ?: ""
        val isMatch = currentService == targetService

        if (!isMatch) {
            logger.info("Skip event for other service: target=$targetService, current=$currentService")
        }
        return isMatch
    }
}
