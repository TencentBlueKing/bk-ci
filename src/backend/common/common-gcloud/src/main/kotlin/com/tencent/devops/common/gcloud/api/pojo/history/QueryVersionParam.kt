package com.tencent.devops.common.gcloud.api.pojo.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class QueryVersionParam(
    @field:JsonProperty("ProductID")
    val productID: String,
    @field:JsonProperty("PubType")
    val pubType: String? = null,
    @field:JsonProperty("QueryVersionStr")
    val queryVersionStr: String? = null
) : ReqParam