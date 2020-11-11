package com.tencent.devops.common.auth.api.external


interface AuthTaskService {
    /**
     * 获取任务创建来源
     */
    fun getTaskCreateFrom(
            taskId: Long
    ): String

    /**
     * 获取任务所属流水线ID
     */
    fun getTaskPipelineId(
        taskId: Long
    ): String

    fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String>

    fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String>

    fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String>

    fun queryPipelineUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String>
}