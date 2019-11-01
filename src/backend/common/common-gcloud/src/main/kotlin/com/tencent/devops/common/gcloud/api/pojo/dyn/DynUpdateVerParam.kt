package com.tencent.devops.common.gcloud.api.pojo.dyn

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class DynUpdateVerParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("ResourceVersion")
    val versionStr: String,
    @field:JsonProperty("ResourceAttr")
    val resourceAttr: Int? = null,
    @field:JsonProperty("ResourceDes")
    val resourceDes: String? = null,
    @field:JsonProperty("ResCustomStr")
    val customStr: String? = null
) : ReqParam