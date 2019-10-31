package com.tencent.devops.common.gcloud.api.pojo.dyn

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class DynDeleteVerParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("ResourceVersion")
    val resourceVersion: String
) : ReqParam