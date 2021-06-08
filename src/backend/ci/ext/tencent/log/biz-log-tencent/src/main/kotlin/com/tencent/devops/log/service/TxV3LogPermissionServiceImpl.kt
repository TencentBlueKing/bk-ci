package com.tencent.devops.log.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxV3LogPermissionServiceImpl @Autowired constructor(
    val client: Client,
    private val tokenCheckService: ClientTokenService
) : LogPermissionService {
    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        val action = permission?.value ?: AuthPermission.VIEW.value
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = action,
            projectCode = projectCode,
            resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
        ).data ?: false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxV3LogPermissionServiceImpl::class.java)
    }
}
