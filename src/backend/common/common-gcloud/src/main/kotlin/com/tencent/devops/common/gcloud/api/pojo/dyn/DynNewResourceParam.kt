package com.tencent.devops.common.gcloud.api.pojo.dyn

import org.springframework.stereotype.Component
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

@Component
data class DynNewResourceParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("ResourceVersion")
    val resourceVersion: String,
    @field:JsonProperty("ResourceName")
    val resourceName: String,
    @field:JsonProperty("VersionInfo")
    val versionInfo: String,
    @field:JsonProperty("ResourceAttr")
    val resourceAttr: Int? = null,
    @field:JsonProperty("ResourceDes")
    val resourceDes: String? = null,
    @field:JsonProperty("ResCustomStr")
    val resCustomStr: String? = null
) : ReqParam