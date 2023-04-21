package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.external.AbstractAuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.auth.pojo.CodeCCAuthServiceCode
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate

class V3AuthExPermissionApi(client: Client,
                            redisTemplate: RedisTemplate<String, String>,
                            private val authTaskService: AuthTaskService,
                            private val codeccAuthPermissionApi: AuthPermissionStrApi)
    : AbstractAuthExPermissionApi(
    client,
    redisTemplate) {

    private val logger = LoggerFactory.getLogger(V3AuthExPermissionApi::class.java)

    private val CODECC_RESOURCE_TYPE = "CODECC_TASK"

    /**
     * 查询指定用户特定权限下的流水线清单
     */
    override fun queryPipelineListForUser(
        user: String,
        projectId: String,
        actions: Set<String>
    ): Set<String> {
        return authTaskService.queryPipelineListForUser(user, projectId)
    }

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    override fun queryTaskListForUser(
        user: String,
        projectId: String,
        actions: Set<String>
    ): Set<String> {
        return try {
            val resultMap = codeccAuthPermissionApi.getUserResourcesByPermissions(
                user = user,
                serviceCode = CodeCCAuthServiceCode(),
                resourceType = CODECC_RESOURCE_TYPE,
                projectCode = projectId,
                permissions = actions) { mutableListOf() }
            val resourceSet = mutableSetOf<String>()
            resultMap.values.forEach {
                if (it.firstOrNull() == "*") {
                    resourceSet.addAll(authTaskService.queryTaskListForUser(user, projectId, actions))
                } else {
                    resourceSet.addAll(it)
                }
            }
            return resourceSet
        } catch (e : Exception){
            logger.error(e.message, e)
            setOf()
        }
    }

    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    override fun queryTaskUserListForAction(
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<String> {
        // v3 auth center not support
        return emptyList()
    }

    /**
     * 批量校验流水线权限
     */
    override fun validatePipelineBatchPermission(
        user: String,
        pipelineId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel> {
        return listOf(BkAuthExResourceActionModel("", "", listOf(), true))
    }

    /**
     * 批量校验代码检查任务权限
     */
    override fun validateTaskBatchPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: Set<String>
    ): List<BkAuthExResourceActionModel> {
        return listOf(BkAuthExResourceActionModel(isPass = true))
    }

    /**
     * 校验工蜂平台权限
     */
    override fun validateGongfengPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: List<CodeCCAuthAction>
    ): Boolean {
        return true
    }

    override fun authProjectManager(projectId: String, user: String): Boolean {
        return true
    }
}