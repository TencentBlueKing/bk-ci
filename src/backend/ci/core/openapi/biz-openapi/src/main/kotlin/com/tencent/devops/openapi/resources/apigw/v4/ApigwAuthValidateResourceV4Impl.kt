package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthValidateResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthValidateResourceV4Impl @Autowired constructor(
    val tokenService: ClientTokenService,
    val client: Client
) : ApigwAuthValidateResourceV4 {

    override fun isProjectUser(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        group: BkAuthGroup?
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_VALIDATE_V4|$userId|is project user|$projectId|$group")
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            group = group
        )
    }

    override fun checkUserInProjectLevelGroup(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_VALIDATE_V4|$userId|check_user_in_project_level_group|$projectId")
        return client.get(ServiceProjectAuthResource::class).checkUserInProjectLevelGroup(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId
        )
    }

    override fun validateUserResourcePermission(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        action: String,
        resourceType: String,
        resourceCode: String
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_VALIDATE_V4|$userId|validate_user_resource_permission|$projectId")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(),
            userId = userId,
            action = action,
            resourceType = resourceType,
            resourceCode = resourceCode,
            projectCode = projectId
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthValidateResourceV4Impl::class.java)
    }
}
