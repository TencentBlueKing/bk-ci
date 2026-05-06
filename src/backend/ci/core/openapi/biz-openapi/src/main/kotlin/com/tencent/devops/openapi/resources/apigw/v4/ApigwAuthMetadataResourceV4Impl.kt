package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceAuthAiResource
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthMetadataResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthMetadataResourceV4Impl @Autowired constructor(
    private val client: Client
) : ApigwAuthMetadataResourceV4 {

    override fun listResourceTypes(
        appCode: String?,
        apigwType: String?,
        userId: String
    ): Result<List<ResourceTypeInfoVo>> {
        logger.info("OPENAPI_AUTH_METADATA_V4|$appCode|$userId|listResourceTypes")
        return client.get(ServiceAuthAiResource::class).listResourceTypes(userId = userId)
    }

    override fun listActions(
        appCode: String?,
        apigwType: String?,
        userId: String,
        resourceType: String
    ): Result<List<ActionInfoVo>> {
        logger.info("OPENAPI_AUTH_METADATA_V4|$appCode|$userId|listActions|$resourceType")
        return client.get(ServiceAuthAiResource::class).listActions(
            userId = userId,
            resourceType = resourceType
        )
    }

    override fun searchResource(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        resourceType: String,
        keyword: String
    ): Result<List<AuthResourceInfo>> {
        logger.info("OPENAPI_AUTH_METADATA_V4|$appCode|$userId|searchResource|$projectId|$resourceType")
        return client.get(ServiceAuthAiResource::class).searchResource(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            keyword = keyword
        )
    }

    override fun getResourceByName(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        resourceType: String,
        resourceName: String
    ): Result<AuthResourceInfo?> {
        logger.info("OPENAPI_AUTH_METADATA_V4|$appCode|$userId|getResourceByName|$projectId|$resourceType")
        return client.get(ServiceAuthAiResource::class).getResourceByName(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceName = resourceName
        )
    }

    override fun getResourceByCode(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Result<AuthResourceInfo?> {
        logger.info("OPENAPI_AUTH_METADATA_V4|$appCode|$userId|getResourceByCode|$projectId|$resourceType")
        return client.get(ServiceAuthAiResource::class).getResourceByCode(
            userId = userId,
            projectId = projectId,
            resourceType = resourceType,
            resourceCode = resourceCode
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwAuthMetadataResourceV4Impl::class.java)
    }
}
