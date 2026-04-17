package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceAuthMetadataResource
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthResourceV4Impl @Autowired constructor(
    val tokenService: ClientTokenService,
    val client: Client
) : ApigwAuthResourceV4 {

    override fun listResourceTypes(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<List<ResourceTypeInfoVo>> {
        logger.info("OPENAPI_AUTH_RESOURCE_V4|list_resource_types")
        return client.get(ServiceAuthMetadataResource::class).listResourceTypes(
            token = tokenService.getSystemToken()
        )
    }

    override fun listActions(
        appCode: String?,
        apigwType: String?,
        resourceType: String,
        userId: String
    ): Result<List<ActionInfoVo>> {
        logger.info("OPENAPI_AUTH_RESOURCE_V4|list_actions|$resourceType")
        return client.get(ServiceAuthMetadataResource::class).listActions(
            token = tokenService.getSystemToken(),
            resourceType = resourceType
        )
    }

    override fun getResourceByName(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        resourceType: String,
        resourceName: String
    ): Result<AuthResourceInfo?> {
        logger.info("OPENAPI_AUTH_RESOURCE_V4|get_resource_by_name|$projectId|$resourceType|$resourceName")
        client.get(ServiceProjectAuthResource::class).checkProjectManagerAndMessage(
            projectId = projectId,
            userId = userId
        )
        return client.get(ServicePermissionAuthResource::class).getResourceByName(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = resourceType,
            resourceName = resourceName
        )
    }

    override fun getResourceByCode(
        appCode: String?,
        apigwType: String?,
        projectId: String,
        userId: String,
        resourceType: String,
        resourceCode: String
    ): Result<AuthResourceInfo?> {
        logger.info("OPENAPI_AUTH_RESOURCE_V4|get_resource_by_code|$projectId|$resourceType|$resourceCode")
        client.get(ServiceProjectAuthResource::class).checkProjectManagerAndMessage(
            projectId = projectId,
            userId = userId
        )
        return client.get(ServicePermissionAuthResource::class).getResourceByCode(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthResourceV4Impl::class.java)
    }
}
