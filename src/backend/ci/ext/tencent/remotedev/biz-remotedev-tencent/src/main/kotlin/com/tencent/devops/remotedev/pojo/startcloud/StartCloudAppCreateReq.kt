package com.tencent.devops.remotedev.pojo.startcloud

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class StartCloudAppCreateReq(
    val contentProviderName: String,
    val appName: String,
    val detail: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StartCloudAppCreateRespData(
    @JsonProperty("AppId")
    val appId: Long
)
