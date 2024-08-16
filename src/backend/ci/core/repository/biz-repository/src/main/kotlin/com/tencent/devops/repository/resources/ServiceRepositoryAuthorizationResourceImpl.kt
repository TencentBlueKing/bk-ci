package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryAuthorizationResource
import com.tencent.devops.repository.service.permission.RepositoryAuthorizationService

@RestResource
class ServiceRepositoryAuthorizationResourceImpl constructor(
    private val repositoryAuthorizationService: RepositoryAuthorizationService
) : ServiceRepositoryAuthorizationResource {
    override fun resetRepositoryAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>> {
        return Result(
            repositoryAuthorizationService.resetRepositoryAuthorization(
                projectId = projectId,
                preCheck = preCheck,
                resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs
            )
        )
    }
}
