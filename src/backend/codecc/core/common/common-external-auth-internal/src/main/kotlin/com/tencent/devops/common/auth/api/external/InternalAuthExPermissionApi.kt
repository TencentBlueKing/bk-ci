package com.tencent.devops.common.auth.api.external

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.UnauthorizedException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.GongfengAuthApi
import com.tencent.devops.common.auth.api.pojo.external.AuthExResponse
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.KEY_BACKEND_ACCESS_TOKEN
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExResourceActionModel
import com.tencent.devops.common.auth.api.pojo.external.model.BkAuthExSingleResourceModel
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExBatchAuthorizedUserRequest
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExBatchPermissionVerityRequest
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExPolicyResourceType
import com.tencent.devops.common.auth.api.pojo.external.request.InternalAuthExResourceListRequest
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.util.OkhttpUtils
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.pojo.git.GitMember

class InternalAuthExPermissionApi(
        client: Client,
        authPropertiesData: AuthExPropertiesData,
        redisTemplate: RedisTemplate<String, String>
) : AbstractAuthExPermissionApi(
        client,
        authPropertiesData,
        redisTemplate) {
    /**
     * 查询指定用户特定权限下的流水线清单
     */
    override fun queryPipelineListForUser(
            user: String,
            projectId: String,
            actions: MutableSet<String>
    ): MutableSet<String> {
        return try{
            queryResourceListForUser(user, projectId, authPropertiesData.pipelineResourceType, authPropertiesData.pipelineServiceCode, actions)
        } catch (e : Exception){
            mutableSetOf()
        }
    }

    /**
     * 查询指定用户特定权限下的代码检查任务清单
     */
    override fun queryTaskListForUser(
            user: String,
            projectId: String,
            actions: MutableSet<String>
    ): MutableSet<String> {
        return try{
            queryResourceListForUser(user, projectId, authPropertiesData.codeccResourceType, authPropertiesData.codeccServiceCode, actions)
        } catch (e : Exception){
            mutableSetOf()
        }


    }

    /**
     * 查询指定代码检查任务下特定权限的用户清单
     */
    override fun queryTaskUserListForAction(
            taskId: String,
            projectId: String,
            actions: MutableSet<String>
    ): List<String> {
        return queryUserListForAction(taskId, projectId, authPropertiesData.codeccResourceType, authPropertiesData.codeccServiceCode, actions)
    }

    /**
     * 查询指定流水线下特定权限的用户清单
     */
    override fun queryPipelineUserListForAction(
            taskId: String,
            projectId: String,
            actions: MutableSet<String>
    ): List<String> {
        return queryUserListForAction(taskId, projectId, authPropertiesData.pipelineResourceType, authPropertiesData.pipelineServiceCode, actions)
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
        return validateBatchPermission(user, pipelineId, projectId, authPropertiesData.pipelineResourceType, authPropertiesData.pipelineServiceCode, actions)
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
        return validateBatchPermission(user, taskId, projectId, authPropertiesData.codeccResourceType, authPropertiesData.codeccServiceCode, actions)
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
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)
        //获取工蜂基本信息
        val gongfengBaseInfo = authTaskService.getGongfengProjInfo(taskId.toLong()) ?: return false
        val gongfengAuthApi = SpringContextUtil.getBean(GongfengAuthApi::class.java)
        //如果是bg的管理员，则直接通过
        if(gongfengAuthApi.authByBgLevelAdmin(user, taskId.toLong()))
            return true

        if (gongfengBaseInfo.id != -1) {
            val haveGongfengPermission = gongfengAuthApi.authByUserIdAndProject(user, gongfengBaseInfo.id)
            if (haveGongfengPermission) {
                return true
            }
        }

        val result: Result<List<GitMember>>
        // 校验工蜂项目权限
        try {
            result =
                    client.getDevopsService(ExternalCodeccRepoResource::class.java).getRepoMembers(gongfengBaseInfo.httpUrl, user)
        } catch (e : Exception) {
            logger.error("", e)
            return false
        }

        // Oauth 权限过期
        if (result.status.toString() == com.tencent.devops.common.api.constant.CommonMessageCode.OAUTH_TOKEN_IS_INVALID) {
            throw CodeCCException(errorCode = CommonMessageCode.OAUTH_TOKEN_IS_INVALID)
        }

        if (result.isNotOk() || result.data == null) {
            return false
        }

        val haveGitPermission = result.data!!
                .stream()
                .map { member -> member.username }
                .anyMatch {member -> member == user}

        logger.info("user {} have gongfeng project permission {}", user, haveGitPermission)
        return haveGitPermission
    }

    override fun authProjectManager(projectId: String, user: String): Boolean {
        val accessToken = getBackendAccessToken()
        val groupValue = "manager"
        logger.info("getProjectUser accessToken:$accessToken")
        val url = "${authPropertiesData.url}/projects/$projectId/users?access_token=$accessToken&group_code=$groupValue"
        val request = Request.Builder().url(url).get().build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("Fail to get project users, url:$url,  $responseContent")
                throw UnauthorizedException("Fail to get project users")
            }

            val responseObject: AuthExResponse<List<String>> =
                    JsonUtil.getObjectMapper().readValue(responseContent, object : TypeReference<AuthExResponse<List<String>>>() {})
            if (responseObject.code != 0) {
                logger.error("Fail to get project users. $responseContent")
                throw UnauthorizedException("Fail to get project users")
            }
            val userList: List<String>? = responseObject.data
            return userList?.contains(user) ?: false
        }
    }

    /**
     * 批量校验权限
     */
    private fun validateBatchPermission(
            user: String,
            resourceCode: String?,
            projectCode: String,
            resourceType: String?,
            serviceCode: String?,
            actions: MutableSet<String>
    ): MutableList<BkAuthExResourceActionModel> {
        val result: AuthExResponse<Map<String, String>> = validateUserBatchPermission(
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                serviceCode = serviceCode,
                userId = user,
                policyCodes = actions
        )
        if (result.code != 0) {
            logger.error("batch authorization failed! user: $user, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("batch authorization failed!")
        }
        val actionAuthResults: MutableList<BkAuthExResourceActionModel> = mutableListOf()
        if (!result.data.isNullOrEmpty()) {
            (result.data)?.forEach {
                val actionAuthResult = BkAuthExResourceActionModel(
                        actionId = it.key,
                        resourceId = listOf(BkAuthExSingleResourceModel(
                                resourceType = resourceType,
                                resourceId = resourceCode
                        )),
                        resourceType = resourceType,
                        isPass = it.value.toLowerCase().toBoolean()
                )
                actionAuthResults.add(actionAuthResult)
            }
        }
        return actionAuthResults
    }

    /**
     * 查询指定资源下特定权限的用户清单
     */
    private fun queryUserListForAction(
            taskId: String,
            projectCode: String,
            resourceType: String?,
            serviceCode: String?,
            actions: MutableSet<String>
    ): List<String> {
        val result: AuthExResponse<Map<String?, List<String>>> = queryAuthorizedUserList(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = taskId,
                serviceCode = serviceCode,
                policyCodes = actions
        )
        if (result.code != 0) {
            logger.error("query user list failed! taskId: $taskId, return code:${result.code}, err message: ${result.message}")
            throw UnauthorizedException("query user list failed!")
        }
        val typeUsersMap: Map<String?, List<String>> = result.data ?: mapOf()
        val users: MutableList<String> = mutableListOf()
        typeUsersMap.forEach {
            if (users.isNullOrEmpty()) {
                users.addAll(it.value)
            } else {
                val iterator = users.iterator()
                while (iterator.hasNext()) {
                    val user = iterator.next()
                    if (!it.value.contains(user)) {
                        iterator.remove()
                    }
                }
            }
        }
        return users
    }

    /**
     * 查询指定用户特定权限下的资源清单
     */
    private fun queryResourceListForUser(
            user: String,
            projectCode: String,
            resourceType: String?,
            serviceCode: String?,
            actions: MutableSet<String>
    ): MutableSet<String> {
        val result: AuthExResponse<MutableSet<String>> = queryResourceList(
                projectCode = projectCode,
                policyCodes = actions,
                resourceType = resourceType,
                serviceCode = serviceCode,
                userId = user
        )
        if (result.code != 0) {
            logger.error("mongorepository resource list failed! projectId: $projectCode, return code:${result.code}, err message: ${result.message}")
            return mutableSetOf()
        }
        return result.data ?: mutableSetOf()
    }

    /**
     * 调用权限中心V1.0 api批量查询有权限用户清单
     */
    private fun queryAuthorizedUserList(
            projectCode: String,
            resourceCode: String,
            resourceType: String?,
            serviceCode: String?,
            policyCodes: MutableSet<String>
    ): AuthExResponse<Map<String?, List<String>>> {
        val url = "${authPropertiesData.url}/permission/project/service/policies/resource/query/users?access_token=" + getBackendAccessToken()
        val bkAuthExBatchAuthorizedUser = InternalAuthExBatchAuthorizedUserRequest(
                projectCode = projectCode,
                serviceCode = serviceCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                policyCodes = policyCodes
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExBatchAuthorizedUser)
        val result = OkhttpUtils.doHttpPost(url, content, emptyMap())
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<Map<String?, List<String>>>>() {})
    }

    /**
     * 调用权限中心1.0 api进行批量权限校验
     */
    private fun validateUserBatchPermission(
            projectCode: String,
            resourceCode: String?,
            resourceType: String?,
            serviceCode: String?,
            userId: String,
            policyCodes: MutableSet<String>
    ): AuthExResponse<Map<String, String>> {
        val url = "${authPropertiesData.url}/permission/project/service/policies/resource/user/batch_verify?access_token=" + getBackendAccessToken()
        val bkAuthExBatchPermissionVerityV1Request = InternalAuthExBatchPermissionVerityRequest(
                projectCode = projectCode,
                serviceCode = serviceCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                userId = userId,
                policyCodes = policyCodes
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExBatchPermissionVerityV1Request)
        val result = OkhttpUtils.doHttpPost(url, content, emptyMap())
        return JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<Map<String, String>>>() {})
    }

    /**
     * 查询资源实例清单
     */
    private fun queryResourceList(
            projectCode: String,
            policyCodes: MutableSet<String>,
            resourceType: String?,
            serviceCode: String?,
            userId: String
    ): AuthExResponse<MutableSet<String>> {
        val url = "${authPropertiesData.url}/permission/project/service/policies/user/query/resources?access_token=" + getBackendAccessToken()
        val policyResourceTypeList: MutableList<InternalAuthExPolicyResourceType> = mutableListOf()
        for (policyCode: String in policyCodes) {
            val policyResourceType = InternalAuthExPolicyResourceType(policyCode, resourceType, listOf())
            policyResourceTypeList.add(policyResourceType)
        }
        val bkAuthExResourceListRequest = InternalAuthExResourceListRequest(
                projectCode = projectCode,
                serviceCode = serviceCode,
                policyResourceTypeList = policyResourceTypeList,
                userId = userId,
                exactResource = 1
        )
        val content = JsonUtil.getObjectMapper().writeValueAsString(bkAuthExResourceListRequest)
        val result = OkhttpUtils.doHttpPost(url, content, emptyMap())
        val response: AuthExResponse<List<InternalAuthExPolicyResourceType>> =
                JsonUtil.getObjectMapper().readValue(result, object : TypeReference<AuthExResponse<List<InternalAuthExPolicyResourceType>>>() {})
        if (response.code != 0) {
            logger.error("mongorepository resource list failed! projectId: $projectCode, return code:${response.code}, err message: ${response.message}")
            throw UnauthorizedException("mongorepository resource list failed!")
        }
        val taskIds: MutableSet<String> = mutableSetOf()
        for (policyResourceType: InternalAuthExPolicyResourceType in response.data!!) {
            taskIds.addAll(policyResourceType.resourceCodeList)
        }

        return AuthExResponse(response.requestId, response.result, response.code, taskIds, response.message)
    }

    /**
     * 查询非用户态Access Token
     */
    private fun getBackendAccessToken(): String {
        return redisTemplate.opsForValue().get(KEY_BACKEND_ACCESS_TOKEN)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InternalAuthExPermissionApi::class.java)
        private const val HTTP_403 = 403
    }
}