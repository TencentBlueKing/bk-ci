package com.tencent.devops.common.gcloud.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class DeleteVerParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("VersionStr")
    val versionStr: String
) : ReqParam