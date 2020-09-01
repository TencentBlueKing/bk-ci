package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.external.AuthExResponse
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_CODE
import com.tencent.devops.common.auth.api.pojo.external.HEADER_APP_SECRET
import com.tencent.devops.common.auth.api.pojo.external.model.*
import com.tencent.devops.common.auth.pojo.external.request.BkAuthExBatchAuthorizedUserRequest
import com.tencent.devops.common.auth.pojo.external.request.BkAuthExBatchPermissionVerityRequest
import com.tencent.devops.common.auth.pojo.external.request.BkAuthExResourceListRequest
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate

class BkAuthExPermissionApi(
        authPropertiesData: AuthExPropertiesData,
        redisTemplate: RedisTemplate<String, String>
) : AbstractAuthExPermissionApi(
        authPropertiesData,
        redisTemplate) {

    override fun authProjectManager(projectId: String, user: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * 查询指定用户特定权限下的流水线清单
     */
    override fun queryPipelineListForUser(
            user: String,
            projectId: String,
            actions: MutableSet<String>
    ): MutableSet<String> {
        return queryResourceListForUser(user, projectId, authPropertiesData.pipelineResourceType, actions)
    }

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    override fun queryTaskListForUser(
            user: String,
            projectId: String,
            actions: MutableSet<String>
    ): MutableSet<String> {
        return queryResourceListForUser(user, projectId, authPropertiesData.codeccResourceType, actions)
    }


    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    override fun queryPipelineUserListForAction(
            taskId: String,
            projectId: String,
            actions: MutableSet<String>
    ): List<String> {
        return queryUserListForAction(taskId, projectId, authPropertiesData.pipelineResourceType, actions)
    }

    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    override fun queryTaskUserListForAction(
            taskId: String,
            projectId: String,
            actions: MutableSet<String>
    ): List<String> {
        return queryUserListForAction(taskId, projectId, authPropertiesData.codeccResourceType, actions)
    }

    /**
     * 批量校验流水线权限
     */
    override fun validatePipelineBatchPermission(
            user: String,
            taskId: String?,
            projectId: String,
            actions: MutableSet<String>
    ): MutableList<BkAuthExResourceActionModel> {
        val pipelineId = getTaskPipelineId(taskId!!.toLong())
        return validateBatchPermission(user, pipelineId, projectId, authPropertiesData.pipelineResourceType, actions)
    }

    /**
     * 批量校验代码检查任务权限
     */
    override fun validateTaskBatchPermission(
            user: String,
            taskId: String?,
            projectId: String,
            actions: MutableSet<String>
    ): MutableList<BkAuthExResourceActionModel> {
        return validateBatchPermission(user, taskId, projectId, authPropertiesData.codeccResourceType, actions)
    }

    /**
     * 校验工蜂权限
     */
    override fun validateGongfengPermission(
        user: String,
        taskId: String,
        projectId: String,
        actions: List<CodeCCAuthAction>
    ): Boolean {
        return false
    }

    /**
     * 批量校验权限
     */
    private fun validateBatchPermission(
            user: String,
            resourceId: String?,
            projectId: String,
            resourceType: String?,
            actions: MutableSet<String>
    ): MutableList<BkAuthExResourceActionModel> {
        val actionList = actions.map {
            BkAuthExResourceActionModel(
                    actionId = it,
                    resourceType = resourceType,
                    resourceId = listOf(BkAuthExSingleResourceModel(
                            resourceType = resourceType!!,
                            resourceId = resourceId
                    ))
            )
        }
        var result: AuthExResponse<List<BkAuthExResourceActionModel>> = validateUserBatchPermission(
                systemId = authPropertiesData.systemId!!,
                principalType = authPropertiesData.principalType!!,
                principalId = user,
                scopeType = authPropertiesData.scopeType!!,
                scopeId = projectId,
                resourcesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("batch authorization failed! user: $user, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("batch authorization failed!")
        }
        return result.data?.toMutableList() ?: mutableListOf()
    }

    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    private fun queryUserListForAction(
            taskId: String,
            projectId: String,
            resourceType: String?,
            actions: MutableSet<String>
    ): List<String> {
        val actionList = actions.map {
            BkAuthExBatchResouceActionModel(
                    actionId = it,
                    resourceType = resourceType,
                    resourceId = listOf(BkAuthExSingleResourceModel(
                            resourceId = taskId,
                            resourceType = resourceType!!
                    ))
            )
        }
        var result: AuthExResponse<List<BkAuthExBatchResouceActionModel>> = queryauthorizedUserList(
                systemId = authPropertiesData.systemId!!,
                scopeType = authPropertiesData.scopeType!!,
                scopeId = projectId,
                resourcesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("mongorepository user list failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("mongorepository user list failed!")
        }
        val typeUsersMap: Map<String?, List<String>> = result.data?.associate {
            it.actionId to (it.principals?.map { bkAuthExPrincipalModel -> bkAuthExPrincipalModel.principalId }
                    ?: listOf())
        }
                ?: mapOf()
        var users: MutableList<String> = mutableListOf()
        typeUsersMap.forEach { users.addAll(it.value) }
        return users
    }

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    private fun queryResourceListForUser(
            user: String,
            projectId: String,
            resourceType: String?,
            actions: MutableSet<String>
    ): MutableSet<String> {
        val actionList = actions.map {
            BkAuthExTypeActionModel(
                    actionId = it,
                    resourceType = resourceType
            )
        }
        val result: AuthExResponse<List<BkAuthExResourceListModel>> = queryResourceList(
                systemId = authPropertiesData.systemId!!,
                principalType = authPropertiesData.principalType!!,
                pricipalId = user,
                scopeType = authPropertiesData.scopeType!!,
                scopeId = projectId,
                resourceTypesActions = actionList
        )
        if (!result.isSuccess()) {
            logger.error("mongorepository resource list failed! projectId: $projectId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("mongorepository resource list failed!")
        }
        return result.data?.let {
            it.map { bkAuthExResourceListModel ->
                bkAuthExResourceListModel.resourceIds?.map { resourceId ->
                    if (resourceId.split(":").size > 1)
                        resourceId.split(":")[1]
                    else
                        "-1"
                } ?: emptyList()
            }.reduce { acc, list -> acc.plus(list) }
        }?.toMutableSet() ?: mutableSetOf()
    }

    /**
     * 调用api批量查询有权限用户清单
     */
    private fun queryauthorizedUserList(
            systemId: String,
            scopeType: String,
            scopeId: String,
            resourcesActions: List<BkAuthExBatchResouceActionModel>
    ): AuthExResponse<List<BkAuthExBatchResouceActionModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${authPropertiesData.url}/bkiam/api/v1/perm/systems/$systemId/resources-perms-principals/search"
        val bkAuthExBatchAuthorizedUser = BkAuthExBatchAuthorizedUserRequest(
                scopeType = scopeType,
                scopeId = scopeId,
                resourcesActions = resourcesActions
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExBatchAuthorizedUser)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to authPropertiesData.codeccCode!!,
                HEADER_APP_SECRET to authPropertiesData.codeccSecret!!
        ))
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<List<BkAuthExBatchResouceActionModel>>>() {})
    }

    /**
     * 调用api进行批量权限校验
     */
    private fun validateUserBatchPermission(
            systemId: String,
            principalType: String,
            principalId: String,
            scopeType: String,
            scopeId: String,
            resourcesActions: List<BkAuthExResourceActionModel>
    ): AuthExResponse<List<BkAuthExResourceActionModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${authPropertiesData.url}/bkiam/api/v1/perm/systems/$systemId/resources-perms/batch-verify"
        val bkAuthExBatchPermissionVerityRequest = BkAuthExBatchPermissionVerityRequest(
                principalType = principalType,
                principalId = principalId,
                scopeType = scopeType,
                scopeId = scopeId,
                resourcesActions = resourcesActions
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExBatchPermissionVerityRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to authPropertiesData.codeccCode!!,
                HEADER_APP_SECRET to authPropertiesData.codeccSecret!!
        ))
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<List<BkAuthExResourceActionModel>>>() {})
    }

    /**
     * 查询资源实例清单
     */
    private fun queryResourceList(
            systemId: String,
            principalType: String,
            pricipalId: String,
            scopeType: String,
            scopeId: String,
            resourceTypesActions: List<BkAuthExTypeActionModel>
    ): AuthExResponse<List<BkAuthExResourceListModel>> {
        if (systemId.isEmpty()) {
            throw UnauthorizedException("system id is null!")
        }
        val url = "${authPropertiesData.url}/bkiam/api/v1/perm/systems/$systemId/authorized-resources/search"
        val bkAuthExResourceListRequest = BkAuthExResourceListRequest(
                principalType = principalType,
                principalId = pricipalId,
                scopeType = scopeType,
                scopeId = scopeId,
                resourceTypesActions = resourceTypesActions
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExResourceListRequest)
        val result = OkhttpUtils.doHttpPost(url, content, mapOf(
                HEADER_APP_CODE to authPropertiesData.codeccCode!!,
                HEADER_APP_SECRET to authPropertiesData.codeccSecret!!
        ))
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<List<BkAuthExResourceListModel>>>() {})
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkAuthExPermissionApi::class.java)
    }
}