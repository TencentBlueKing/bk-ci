package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.auth.enums.ResourceAuthorizationHandoverStatus
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.service.ServicePipelineAuthorizationResource
import com.tencent.devops.process.permission.PipelineAuthorizationService

@RestResource
class ServicePipelineAuthorizationResourceImpl constructor(
    private val pipelineAuthorizationService: PipelineAuthorizationService
) : ServicePipelineAuthorizationResource {
    override fun resetPipelineAuthorization(
        projectId: String,
        preCheck: Boolean,
        resourceAuthorizationHandoverDTOs: List<ResourceAuthorizationHandoverDTO>
    ): Result<Map<ResourceAuthorizationHandoverStatus, List<ResourceAuthorizationHandoverDTO>>> {
        return Result(
            pipelineAuthorizationService.resetPipelineAuthorization(
                projectId = projectId,
                preCheck = preCheck,
                resourceAuthorizationHandoverDTOs = resourceAuthorizationHandoverDTOs
            )
        )
    }
}
