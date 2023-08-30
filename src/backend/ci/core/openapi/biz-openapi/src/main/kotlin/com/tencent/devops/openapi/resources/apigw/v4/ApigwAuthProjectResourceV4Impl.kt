package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.vo.ProjectPermissionInfoVO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthProjectResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthProjectResourceV4Impl @Autowired constructor(
    val tokenService: ClientTokenService,
    val client: Client
) : ApigwAuthProjectResourceV4 {
    companion object {
        val logger = LoggerFactory.getLogger(ApigwAuthProjectResourceV4Impl::class.java)
    }

    override fun getProjectPermissionInfo(
        appCode: String?,
        apigwType: String?,
        projectId: String
    ): Result<ProjectPermissionInfoVO> {
        logger.info("OPENAPI_AUTH_PROJECT_PERMISSION_INFO_V4|$projectId")
        return client.get(ServiceProjectAuthResource::class).getProjectPermissionInfo(
            token = tokenService.getSystemToken(null)!!,
            projectCode = projectId
        )
    }
}
