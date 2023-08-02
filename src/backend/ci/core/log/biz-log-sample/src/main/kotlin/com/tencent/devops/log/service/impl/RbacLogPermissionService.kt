package com.tencent.devops.log.service.impl

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
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
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = RbacAuthUtils.buildAction(
                permission ?: AuthPermission.VIEW, AuthResourceType.PIPELINE_DEFAULT
            ),
            projectCode = projectCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            resourceCode = pipelineId,
            relationResourceType = null
        ).data ?: false
    }
}
