package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源授权回调返回实体DTO")
data class ListResourcesAuthorizationDTO(
    val data: BaseDataResponseDTO<ResourceAuthorizationResponse>
) : CallbackBaseResponseDTO() {
    fun buildResourcesAuthorizationListFailResult(message: String): ListResourcesAuthorizationDTO {
        this.code = 0
        this.message = message
        this.data.count = 0
        return this
    }

    fun buildResourcesAuthorizationListResult(): ListResourcesAuthorizationDTO {
        this.code = 0L
        this.message = ""
        return this
    }
}
