package com.tencent.devops.common.task.pojo

/**
 * 任务执行结果数据类
 *
 * @property taskId 任务唯一标识符
 * @property success 标识任务是否成功执行
 * @property result 任务执行的结果详情（默认为空字符串）
 */
data class TaskResult(
    val taskId: String,
    val success: Boolean,
    val result: String = ""
)