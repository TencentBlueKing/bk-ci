package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.service.ServiceSignatureManageResource
import com.tencent.devops.project.pojo.UserSignatureStatusResponse
import com.tencent.devops.project.service.SignatureManageService

@RestResource
class ServiceSignatureManageResourceImpl(
    val signatureManageService: SignatureManageService
) : ServiceSignatureManageResource {
    override fun fetchLiveSignatureStatus(
        userId: String,
        projectId: String
    ): Result<UserSignatureStatusResponse> {
        return Result(
            signatureManageService.fetchLiveSignatureStatus(
                userId = userId,
                projectId = projectId
            )
        )
    }

    override fun getSignatureStatus(userId: String, projectId: String): Result<UserSignatureStatusResponse> {
        return Result(
            signatureManageService.getSignatureStatus(
                projectId = projectId,
                userId = userId
            )
        )
    }

    override fun listSignatureProjects(userId: String): Result<List<String>> {
        return Result(signatureManageService.listSignatureProjects())
    }
}
