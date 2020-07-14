package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import org.slf4j.LoggerFactory

class V3PipelinePermissionService constructor(
    authProjectApi: AuthProjectApi,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    pipelineAuthServiceCode: PipelineAuthServiceCode
) : AbstractPipelinePermissionService(
    authProjectApi = authProjectApi,
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    pipelineAuthServiceCode = pipelineAuthServiceCode
) {

    override fun checkPipelinePermission(userId: String, projectId: String, permission: AuthPermission): Boolean {
        logger.info("checkPipelinePermission only check action project[$projectId]")
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = pipelineAuthServiceCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT,
            projectCode = projectId,
            resourceCode = projectId,
            permission = AuthPermission.CREATE,
            relationResourceType = AuthResourceType.PROJECT
        )
    }

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}