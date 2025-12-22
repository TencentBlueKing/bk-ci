package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvNodeAuthorizationResource
import com.tencent.devops.environment.permission.EnvNodeAuthorizationService
import com.tencent.devops.environment.pojo.EnvData
import com.tencent.devops.environment.service.EnvService

@RestResource
class ServiceEnvNodeAuthorizationResourceImpl constructor(
    private val envNodeAuthorizationService: EnvNodeAuthorizationService,
    private val envService: EnvService
) : ServiceEnvNodeAuthorizationResource {
    override fun resetEnvNodeAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>> {
        return Result(
            envNodeAuthorizationService.resetEnvNodeAuthorization(
                projectId = projectId,
                preCheck = preCheck,
                resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs
            )
        )
    }

    override fun fetchAllNodeEnvList(userId: String, projectId: String, workspaceName: String): Result<List<EnvData>> {
        return Result(envService.fetchAllNodeEnvList(userId, projectId, workspaceName))
    }
}
