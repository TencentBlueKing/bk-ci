package com.tencent.devops.common.auth.api.pojo.external.callback

import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO

class FetchInstanceInfo : FetchInstanceInfoResponseDTO() {

    fun buildFetchInstanceFailResult(): FetchInstanceInfoResponseDTO {
        val result = FetchInstanceInfoResponseDTO()
        result.code = 0
        result.message = "empty data"
        result.data = emptyList()
        return result
    }

    fun buildFetchInstanceResult(data: List<InstanceInfoDTO>): FetchInstanceInfoResponseDTO {
        val result = FetchInstanceInfoResponseDTO()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }
}
