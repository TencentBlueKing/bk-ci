package com.tencent.devops.log.service.impl

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.rbac.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.log.service.LogPermissionService
import org.springframework.beans.factory.annotation.Autowired

class RbacLogPermissionService @Autowired constructor(
    val client: Client,
    private val tokenCheckService: ClientTokenService
) : LogPermissionService {

    override fun verifyUserLogPermission(
        projectCode: String,
        userId: String,
        permission: AuthPermission?,
        authResourceType: AuthResourceType?
    ): Boolean {
        val finalAuthResourceType = authResourceType ?: AuthResourceType.PIPELINE_DEFAULT
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(),
            action = RbacAuthUtils.buildAction(
                permission ?: AuthPermission.VIEW, finalAuthResourceType
            ),
            projectCode = projectCode,
            resourceCode = finalAuthResourceType.value
        ).data ?: false
    }

    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?,
        authResourceType: AuthResourceType?
    ): Boolean {
        val finalAuthResourceType = authResourceType ?: AuthResourceType.PIPELINE_DEFAULT
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken(),
            action = RbacAuthUtils.buildAction(
                permission ?: AuthPermission.VIEW, finalAuthResourceType
            ),
            projectCode = projectCode,
            resourceType = finalAuthResourceType.value,
            resourceCode = pipelineId,
            relationResourceType = null
        ).data ?: false
    }
}
