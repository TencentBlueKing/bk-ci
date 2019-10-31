package com.tencent.devops.common.gcloud.api.pojo.history

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class NewUploadTaskParam(
    @field:JsonProperty("Uin")
    val uin: String,
    @field:JsonProperty("ProductID")
    val productId: Int,
    @field:JsonProperty("VersionStr")
    val versionStr: String,
    @field:JsonProperty("VersionType")
    val versionType: Int,
    @field:JsonProperty("DiffVersions")
    val diffVersions: String? = null,
    @field:JsonProperty("ResMode")
    val resMode: Int? = null,
    @field:JsonProperty("UseHttps")
    val useHttps: Int? = null,
    @field:JsonProperty("RegionID")
    val regionId: Int? = null
) : ReqParam