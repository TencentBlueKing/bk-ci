package com.tencent.devops.common.gcloud.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class NewAppParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("VersionStr")
    val versionStr: String,
    @field:JsonProperty("VersionInfo")
    val versionInfo: String,
    @field:JsonProperty("DiffVersions")
    val diffVersions: Int? = null,
    @field:JsonProperty("AvailableType")
    val availableType: Int? = null,
    @field:JsonProperty("GrayRuleID")
    val grayRuleID: String? = null,
    @field:JsonProperty("VersionDes")
    val versionDes: String? = null,
    @field:JsonProperty("CustomStr")
    val customStr: String? = null
) : ReqParam