package com.tencent.devops.common.task.util

import com.tencent.devops.common.task.pojo.TaskTypeEnum

object BatchTaskUtil {

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
}
