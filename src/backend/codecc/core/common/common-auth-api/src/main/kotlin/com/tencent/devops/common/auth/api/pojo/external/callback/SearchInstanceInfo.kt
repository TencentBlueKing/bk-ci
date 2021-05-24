package com.tencent.devops.common.auth.api.pojo.external.callback

import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO

class SearchInstanceInfo : SearchInstanceResponseDTO() {

    fun buildSearchInstanceFailResult(): SearchInstanceInfo {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        val result = SearchInstanceInfo()
        result.code = 0
        result.message = "empty data"
        result.data = data
        return result
    }

    // 关键词太短
    fun buildSearchInstanceKeywordFailResult(): SearchInstanceInfo {
        val result = SearchInstanceInfo()
        result.code = AuthConstants.KEYWORD_SHORT
        result.message = AuthConstants.KEYWORD_SHORT_MESSAGE
        return result
    }

    // 关键词匹配中数据太多，需要优化关键词
    fun buildSearchInstanceResultFailResult(): SearchInstanceInfo {
        val result = SearchInstanceInfo()
        result.code = AuthConstants.TOO_RESULT_DATA
        result.message = AuthConstants.TOO_RESULT_DATA_MESSAGE
        return result
    }

    fun buildSearchInstanceResult(infos: List<InstanceInfoDTO>, count: Long): SearchInstanceInfo {
        val data = BaseDataResponseDTO<InstanceInfoDTO>()
        data.count = count
        data.result = infos
        val result = SearchInstanceInfo()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }
}
