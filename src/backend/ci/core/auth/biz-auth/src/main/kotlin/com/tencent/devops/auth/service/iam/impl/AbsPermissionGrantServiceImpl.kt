package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.grant.GrantPathDTO
import com.tencent.bk.sdk.iam.dto.grant.GrantResourceDTO
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.service.iam.PermissionGrantService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class AbsPermissionGrantServiceImpl @Autowired constructor(
    open val grantServiceImpl: GrantServiceImpl,
    open val iamConfiguration: IamConfiguration,
    open val client: Client
) : PermissionGrantService {
    override fun grantInstancePermission(
        projectId: String,
        grantInfo: GrantInstanceDTO
    ): Boolean {
        val pathList = mutableListOf<GrantPathDTO>()
        if (grantInfo.resourceType == AuthResourceType.PROJECT.value) {
            pathList.add(GrantPathDTO.builder().id(projectId)
                .type(grantInfo.resourceType)
                .name(getProjectName(projectId))
                .build())
        } else {
            // 非项目类的资源都需绑定项目
            pathList.add(GrantPathDTO.builder().id(projectId)
                .type(AuthResourceType.PROJECT.value)
                .name(getProjectName(projectId))
                .build())
            pathList.add(GrantPathDTO.builder().id(grantInfo.resourceCode)
                .type(grantInfo.resourceType).name(grantInfo.resourceName).build())
        }

        val resources = mutableListOf<GrantResourceDTO>()
        val resource = GrantResourceDTO.builder().system(iamConfiguration.systemId).type(grantInfo.resourceType)
            .path(pathList).build()
        resources.add(resource)
        try {
            val grantResult = grantServiceImpl.grantPermission(
                grantInfo.createUser,
                grantInfo.permission,
                resources
            )
            logger.info("Grant instance success: grantResult = $grantResult")
        } catch (e: Exception) {
            logger.warn("Grant instance fail: $e")
            return false
        }
        return true
    }

    private fun getProjectName(projectId: String): String {
        return (client.get(ServiceProjectResource::class).get(projectId).data ?: return "").projectName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionGrantServiceImpl::class.java)
    }
}
