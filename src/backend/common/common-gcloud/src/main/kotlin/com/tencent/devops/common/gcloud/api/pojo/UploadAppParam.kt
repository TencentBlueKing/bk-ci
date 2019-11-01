package com.tencent.devops.common.gcloud.api.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.param.ReqParam

data class UploadAppParam(
    @field:JsonProperty("productid")
    var productId: Int,
    @field:JsonProperty("versionstr")
    val versionStr: String,
    val md5: String,
    @field:JsonProperty("diffversions")
    val diffVersions: String? = null,
    @field:JsonProperty("regionid")
    val regionId: Int? = null,
    @field:JsonProperty("skipupload")
    val skipUpload: Int? = null,
    @field:JsonProperty("downloadlink")
    val downloadLink: String? = null,
    val https: String? = null
) : ReqParam