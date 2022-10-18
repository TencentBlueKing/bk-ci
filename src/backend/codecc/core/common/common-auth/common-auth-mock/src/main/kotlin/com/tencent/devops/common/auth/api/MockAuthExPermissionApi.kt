package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AbstractAuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.client.Client
import org.springframework.data.redis.core.RedisTemplate

class MockAuthExPermissionApi(client: Client,
                              redisTemplate: RedisTemplate<String, String>,
                              private val authTaskService: AuthTaskService)
    : AbstractAuthExPermissionApi(
    client,
    redisTemplate) {



    override fun queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return authTaskService.queryPipelineListForUser(user, projectId, actions)
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        return authTaskService.queryTaskListForUser(user, projectId, actions)
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        return authTaskService.queryTaskUserListForAction(taskId, projectId, actions)
    }

    override fun validatePipelineBatchPermission(user: String, pipelineId: String, projectId: String, actions: Set<String>): List<BkAuthExResourceActionModel> {
        return listOf(BkAuthExResourceActionModel("", "", listOf(), true))
    }

    override fun validateTaskBatchPermission(user: String, taskId: String, projectId: String, actions: Set<String>): List<BkAuthExResourceActionModel> {
        return listOf(BkAuthExResourceActionModel("", "", listOf(), true))
    }

    override fun validateGongfengPermission(user: String, taskId: String, projectId: String, actions: List<CodeCCAuthAction>): Boolean {
        return true
    }

    override fun authProjectManager(projectId: String, user: String): Boolean {
        return true
    }
}