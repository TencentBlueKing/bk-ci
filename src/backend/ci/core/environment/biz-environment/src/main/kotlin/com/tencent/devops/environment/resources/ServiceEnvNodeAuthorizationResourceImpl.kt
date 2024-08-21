package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceEnvNodeAuthorizationResource
import com.tencent.devops.environment.permission.EnvNodeAuthorizationService

@RestResource
class ServiceEnvNodeAuthorizationResourceImpl constructor(
    private val envNodeAuthorizationService: EnvNodeAuthorizationService
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
}
