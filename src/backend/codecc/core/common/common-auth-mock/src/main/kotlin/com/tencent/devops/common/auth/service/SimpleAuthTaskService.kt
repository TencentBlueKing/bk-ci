package com.tencent.devops.common.auth.service

import com.tencent.devops.common.auth.api.external.AuthTaskService
import org.springframework.stereotype.Service

@Service
class SimpleAuthTaskService: AuthTaskService {
    override fun getTaskCreateFrom(taskId: Long): String {
        return ""
    }

    override fun getTaskPipelineId(taskId: Long): String {
        return ""
    }

    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return emptySet()
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return emptySet()
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        return emptyList()
    }

    override fun queryPipelineUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        return emptyList()
    }
}