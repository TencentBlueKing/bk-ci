package com.tencent.devops.common.gcloud.api.pojo.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class NewResParam(
    @field:JsonProperty("AppVersionStr")
    val appVersionStr: String,
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productID: Int,
    @field:JsonProperty("VersionStr")
    val versionStr: String,
    @field:JsonProperty("VersionInfo")
    val versionInfo: String,
    @field:JsonProperty("AvailableType")
    val availableType: Int? = null,
    @field:JsonProperty("GrayRuleID")
    val grayRuleID: Int? = null,
    @field:JsonProperty("VersionDes")
    val versionDes: String? = null,
    @field:JsonProperty("CustomStr")
    val customStr: String? = null
) : ReqParam