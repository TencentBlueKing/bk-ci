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
        if (targetService == null) {
            return false
        }
        // 获取当前运行的服务名称
        val currentService = BkServiceUtil.getApplicationName() ?: ""
        // 服务名称不匹配时记录日志并返回false
        return if (currentService != targetService) {
            logger.info("Skip event for other service: target=$targetService, current=$currentService")
            false
        } else {
            true
        }
    }
}
