package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OpSignaturePlatformResource
import com.tencent.devops.project.pojo.SignaturePlatformDetails
import com.tencent.devops.project.pojo.SignaturePlatformUpdateRequest
import com.tencent.devops.project.service.SignatureManageService

@RestResource
class OpSignaturePlatformResourceImpl(
    private val signatureManageNewService: SignatureManageService
) : OpSignaturePlatformResource {
    override fun createOrUpdate(details: SignaturePlatformDetails): Result<Boolean> {
        signatureManageNewService.createOrUpdatePlatform(details)
        return Result(true)
    }

    override fun updateInformation(platform: String, request: SignaturePlatformUpdateRequest): Result<Boolean> {
        signatureManageNewService.updatePlatformInformation(platform, request)
        return Result(true)
    }

    override fun delete(platform: String): Result<Boolean> {
        signatureManageNewService.deletePlatform(platform)
        return Result(true)
    }
}
