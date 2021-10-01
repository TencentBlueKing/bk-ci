package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.grant.GrantPathDTO
import com.tencent.bk.sdk.iam.dto.grant.GrantResourceDTO
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.service.iam.PermissionGrantService
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

open class AbsPermissionGrantServiceImpl @Autowired constructor(
    val grantServiceImpl: GrantServiceImpl,
    val iamConfiguration: IamConfiguration
): PermissionGrantService {
    override fun grantInstancePermission(
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Boolean {
        val pathList = mutableListOf<GrantPathDTO>()
        if (resourceType == AuthResourceType.PROJECT.value) {
            pathList.add(GrantPathDTO.builder().id(projectId).type(resourceType).name("").build())
        } else {
            // 非项目类的资源都需绑定项目
            pathList.add(GrantPathDTO.builder().id(projectId).type(AuthResourceType.PROJECT.value).name("").build())
            pathList.add(GrantPathDTO.builder().id(resourceCode).type(resourceType).name("").build())
        }

        val resources = mutableListOf<GrantResourceDTO>()
        val resource = GrantResourceDTO.builder().system(iamConfiguration.systemId).type(resourceType)
            .path(pathList).build()
        resources.add(resource)
        try {
            val grantResult = grantServiceImpl.grantPermission(
                userId,
                action,
                resources
            )
            logger.info("grantInstance success: $grantResult")
        } catch (e: Exception) {
            logger.warn("grantInstance fail: $e")
            return false
        }
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionGrantServiceImpl::class.java)
    }
}
