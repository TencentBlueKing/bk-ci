package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthGrantResourceV4
import com.tencent.devops.openapi.service.OpenapiPermissionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthGrantResourceV4Impl @Autowired constructor(
    private val client: Client,
    private val tokenService: ClientTokenService,
    private val openapiPermissionService: OpenapiPermissionService
) : ApigwAuthGrantResourceV4 {
    override fun grantInstancePermission(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        grantInstance: GrantInstanceDTO
    ): Result<Boolean> {
        logger.info("OPENAPI_AUTH_GRANT_V4|$userId|grant instance permission|$projectId|$grantInstance")
        openapiPermissionService.validProjectManagerPermission(appCode, apigwType, userId, projectId)
        return Result(
            client.get(ServicePermissionAuthResource::class).grantInstancePermission(
                userId = userId,
                projectCode = projectId,
                grantInstance = grantInstance,
                token = tokenService.getSystemToken(null)!!
            ).data ?: false
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthGrantResourceV4Impl::class.java)
    }
}
