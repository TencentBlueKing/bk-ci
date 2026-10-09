package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthPermissionResourceV4
import com.tencent.devops.openapi.pojo.auth.ListUserResourcesByPermissionsReq
import com.tencent.devops.openapi.pojo.auth.UserResourceByActionVO
import com.tencent.devops.openapi.pojo.auth.UserResourcesByPermissionsVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthPermissionResourceV4Impl @Autowired constructor(
    private val tokenService: ClientTokenService,
    private val client: Client
) : ApigwAuthPermissionResourceV4 {

    override fun listUserResourcesByPermissions(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        request: ListUserResourcesByPermissionsReq
    ): Result<UserResourcesByPermissionsVO> {
        val queryUserId = request.userId?.takeIf { it.isNotBlank() } ?: userId
        if (queryUserId.isBlank()) {
            throw ParamBlankException("userId cannot be blank")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("projectId cannot be blank")
        }
        if (request.resourceType.isBlank()) {
            throw ParamBlankException("resourceType cannot be blank")
        }
        val actions = request.actions.map { it.trim() }.filter { it.isNotBlank() }
        if (actions.isEmpty()) {
            throw ParamBlankException("actions cannot be empty")
        }

        logger.info(
            "OPENAPI_AUTH_PERMISSION_V4|$appCode|$queryUserId|" +
                "listUserResourcesByPermissions|$projectId|${request.resourceType}|$actions"
        )
        val authResult = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            userId = queryUserId,
            token = tokenService.getSystemToken(),
            type = null,
            action = actions,
            projectCode = projectId,
            resourceType = request.resourceType
        )
        if (authResult.isNotOk()) {
            return Result(status = authResult.status, message = authResult.message ?: "")
        }
        val permissionMap = authResult.data ?: emptyMap()
        return Result(
            UserResourcesByPermissionsVO(
                userId = queryUserId,
                projectId = projectId,
                resourceType = request.resourceType,
                resources = actions.map { action ->
                    toUserResourceByAction(action = action, permissionMap = permissionMap)
                }
            )
        )
    }

    private fun toUserResourceByAction(
        action: String,
        permissionMap: Map<AuthPermission, List<String>>
    ): UserResourceByActionVO {
        val permission = action.substringAfterLast("_")
        val authPermission = AuthPermission.get(permission)
        return UserResourceByActionVO(
            action = action,
            permission = authPermission.value,
            resourceCodes = permissionMap[authPermission] ?: emptyList()
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwAuthPermissionResourceV4Impl::class.java)
    }
}
