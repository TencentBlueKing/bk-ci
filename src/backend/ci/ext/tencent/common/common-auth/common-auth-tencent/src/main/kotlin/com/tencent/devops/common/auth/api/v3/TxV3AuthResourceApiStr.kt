package com.tencent.devops.common.auth.api.v3

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthResourceApiStr
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.service.IamEsbService
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import org.springframework.beans.factory.annotation.Autowired

class TxV3AuthResourceApiStr @Autowired constructor(
    val iamEsbService: IamEsbService,
    val iamConfiguration: IamConfiguration,
    val client: Client,
    val tokenService: ClientTokenService
) : AuthResourceApiStr {
    override fun createGrantResource(
        user: String,
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String,
        resourceName: String,
        authGroupList: List<BkAuthGroup>?
    ) {
        createResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun batchCreateResource(
        principalId: String,
        scopeType: String,
        scopeId: String,
        resourceType: String,
        resourceList: List<ResourceRegisterInfo>,
        systemId: String
    ): Boolean {
        return true
    }

    override fun deleteResource(
        scopeType: String,
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun modifyResource(
        scopeType: String,
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun createResource(
        scopeType: String,
        user: String,
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        createResource(
            user = user,
            serviceCode = serviceCode,
            resourceType = resourceType,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceName = resourceName
        )
    }

    override fun createResource(
        user: String,
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) {
        // 如果已经有项目的all_action权限,就无需重复添加具体到实例的权限
        val hasAllAction = client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId = user,
            resourceCode = resourceType,
            projectCode = projectCode,
            action = "all_action",
            token = tokenService.getSystemToken(null)!!
        ).data
        if (hasAllAction != null && hasAllAction) {
            return
        }

        // 新建关联, 会创建对应action的权限以及该action相关的权限
//        val ancestors = mutableListOf<AncestorsApiReq>()
//        if (resourceType != AuthResourceType.PROJECT.value) {
//            ancestors.add(AncestorsApiReq(
//                system = iamConfiguration.systemId,
//                id = projectCode,
//                type = AuthResourceType.PROJECT.value
//            ))
//        }
//        val iamApiReq = EsbCreateApiReq(
//            creator = user,
//            name = resourceName,
//            id = resourceCode,
//            type = resourceType,
//            system = iamConfiguration.systemId,
//            ancestors = ancestors,
//            bk_app_code = "",
//            bk_app_secret = "",
//            bk_username = user
//        )
//        iamEsbService.createRelationResource(iamApiReq)
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            token = tokenService.getSystemToken(null)!!,
            userId = user,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            projectCode = projectCode
        )
    }

    override fun modifyResource(
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    ) = Unit

    override fun deleteResource(
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        resourceCode: String
    ) = Unit

    override fun batchCreateResource(
        serviceCode: String?,
        resourceType: String,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    ) = Unit
}
