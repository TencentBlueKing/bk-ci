package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceListDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceListDTO

class FetchInstanceListInfo : FetchInstanceListDTO<FetchInstanceListData>() {
    fun buildFetchInstanceListFailResult(message: String): FetchInstanceListDTO<FetchInstanceListData> {
        val data = BaseDataResponseDTO<InstanceListDTO<FetchInstanceListData>>()
        val result = FetchInstanceListDTO<FetchInstanceListData>()
        result.code = 0
        result.message = message
        result.data = data
        return result
    }

    fun buildFetchInstanceListResult(
        infos: List<InstanceListDTO<FetchInstanceListData>>,
        count: Long
    ): FetchInstanceListDTO<FetchInstanceListData> {
        val data = BaseDataResponseDTO<InstanceListDTO<FetchInstanceListData>>()
        data.count = count
        data.result = infos
        val result = FetchInstanceListDTO<FetchInstanceListData>()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }
}
