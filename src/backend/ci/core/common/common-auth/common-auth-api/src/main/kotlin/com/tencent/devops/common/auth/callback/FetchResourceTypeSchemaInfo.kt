package com.tencent.devops.common.auth.callback

import com.tencent.bk.sdk.iam.dto.callback.response.FetchResourceTypeSchemaDTO
import com.tencent.bk.sdk.iam.dto.callback.response.SchemaData

class FetchResourceTypeSchemaInfo : FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties>() {
    fun buildFetchResourceTypeSchemaResult(data: SchemaData<FetchResourceTypeSchemaProperties>):
        FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties> {
        val result = FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties>()
        result.code = 0L
        result.message = ""
        result.data = data
        return result
    }

    fun buildFetchResourceTypeSchemaFailResult(): FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties> {
        val result = FetchResourceTypeSchemaDTO<FetchResourceTypeSchemaProperties>()
        result.code = 0
        result.message = "empty data"
        result.data = null
        return result
    }
}
