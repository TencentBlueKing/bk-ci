package com.tencent.devops.common.auth.api

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.external.AbstractAuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.auth.pojo.GithubAuthProperties
import com.tencent.devops.common.auth.utils.AuthActionConvertUtils
import com.tencent.devops.common.client.Client
import org.springframework.data.redis.core.RedisTemplate

class GithubAuthExPermissionApi(client: Client,
                                redisTemplate: RedisTemplate<String, String>,
                                private val authTaskService: AuthTaskService,
                                private val properties : GithubAuthProperties)
    : AbstractAuthExPermissionApi(
    client,
    redisTemplate) {

    override fun  queryPipelineListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val result = client.getDevopsService(ServicePermissionAuthResource::class.java)
            .getUserResourcesByPermissions(
                user, properties.token ?: "", actions.toList(), projectId,
                properties.pipelineResourceType ?: "pipeline"
            )
        if (result.isNotOk() || result.data.isNullOrEmpty()) {
            return emptySet()
        }
        result.data!!.all { entry -> entry.value.contains("*")}
        return authTaskService.queryPipelineListByProjectId(projectId)
    }

    override fun queryTaskListForUser(user: String, projectId: String, actions: Set<String>): Set<String> {
        val codeccActions = actions.mapNotNull { it ->
            var action: CodeCCAuthAction? = null
            for (value in CodeCCAuthAction.values()) {
                if (value.actionName == it) {
                    action = value
                    break
                }
            }
            action
        }.toList()
        val pipelineActions = AuthActionConvertUtils.covert(codeccActions).map { it.actionName }.toSet()
        val pipelineIds = queryPipelineListForUser(user, projectId, pipelineActions)
        return if (pipelineIds.isEmpty()) {
            emptySet()
        } else {
            authTaskService.queryTaskListByPipelineIds(pipelineIds)
        }
    }

    override fun queryTaskUserListForAction(taskId: String, projectId: String, actions: Set<String>): List<String> {
        return authTaskService.queryTaskUserListForAction(taskId, projectId, actions)
    }

    override fun validatePipelineBatchPermission(user: String, pipelineId: String, projectId: String, actions: Set<String>): List<BkAuthExResourceActionModel> {
        val pipelineIds = queryPipelineListForUser(user,projectId,actions)
        if(pipelineIds.isNotEmpty() && pipelineIds.contains(pipelineId)){
            return listOf(BkAuthExResourceActionModel("", "", listOf(), true))
        }
        return listOf(BkAuthExResourceActionModel("", "", listOf(), false))
    }

    override fun validateTaskBatchPermission(user: String, taskId: String, projectId: String, actions: Set<String>): List<BkAuthExResourceActionModel> {
        val taskIds = queryTaskListForUser(user,projectId,actions)
        if(taskIds.isNotEmpty() && taskIds.contains(taskId)){
            return listOf(BkAuthExResourceActionModel("", "", listOf(), true))
        }
        return listOf(BkAuthExResourceActionModel(isPass = true))
    }

    override fun validateGongfengPermission(user: String, taskId: String, projectId: String, actions: List<CodeCCAuthAction>): Boolean {
        return true
    }

    override fun authProjectManager(projectId: String, user: String): Boolean {
        return false
    }


}