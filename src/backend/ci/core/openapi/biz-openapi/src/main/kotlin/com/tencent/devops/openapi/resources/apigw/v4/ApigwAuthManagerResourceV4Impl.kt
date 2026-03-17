package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.auth.api.service.ServiceResourceMemberResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwAuthManagerResourceV4
import com.tencent.devops.openapi.pojo.ProjectManagerRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwAuthManagerResourceV4Impl @Autowired constructor(
    val tokenService: ClientTokenService,
    val client: Client
) : ApigwAuthManagerResourceV4 {
    companion object {
        val logger =
            LoggerFactory.getLogger(ApigwAuthManagerResourceV4Impl::class.java)
    }

    override fun getProjectManagers(
        appCode: String?,
        apigwType: String?,
        request: ProjectManagerRequest
    ): Result<List<String>> {
        logger.info(
            "OPENAPI_AUTH_MANAGER_V4" +
                " getProjectManagers|$appCode|${request.projectId}"
        )
        val maxManagerCount = 5
        return client.get(ServiceResourceMemberResource::class)
            .getResourceGroupMembers(
                token = tokenService.getSystemToken(),
                projectCode = request.projectId,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = request.projectId,
                group = BkAuthGroup.MANAGER
            ).let { result ->
                Result(result.data?.take(maxManagerCount) ?: emptyList())
            }
    }
}
