package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AbstractAuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthExPropertiesData
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.client.Client
import org.springframework.data.redis.core.RedisTemplate

class MockAuthExPermissionApi(client: Client, redisTemplate: RedisTemplate<String, String>)
    : AbstractAuthExPermissionApi(client, redisTemplate) {
    override fun queryPipelineListForUser(user: String, projectId: String, actions: MutableSet<String>): MutableSet<String> {
        return mutableSetOf()
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: MutableSet<String>): MutableSet<String> {
        return mutableSetOf()
    }

    override fun queryPipelineUserListForAction(taskId: String, projectId: String, actions: MutableSet<String>): List<String> {
        return emptyList()
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: MutableSet<String>): List<String> {
        return emptyList()
    }

    override fun validatePipelineBatchPermission(user: String, taskId: String?, projectId: String, actions: MutableSet<String>): MutableList<BkAuthExResourceActionModel> {
        return mutableListOf()
    }

    override fun validateTaskBatchPermission(user: String, taskId: String?, projectId: String, actions: MutableSet<String>): MutableList<BkAuthExResourceActionModel> {
        return mutableListOf()
    }

    override fun validateGongfengPermission(user: String, taskId: String, projectId: String, actions: List<CodeCCAuthAction>): Boolean {
        return true
    }

    override fun authProjectManager(projectId: String, user: String): Boolean {
        return true
    }
}