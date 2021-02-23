package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO

class ListInstanceInfo : ListInstanceResponseDTO() {

    fun buildListInstanceFailResult(): ListInstanceResponseDTO {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        val result = ListInstanceResponseDTO()
        result.code = 0
        result.message = "empty data"
        result.data = data
        return result
    }

    fun buildListInstanceResult(infos: List<InstanceInfoDTO>, count: Long): ListInstanceResponseDTO {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = count
        data.result = infos
        val result = ListInstanceResponseDTO()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }
}
