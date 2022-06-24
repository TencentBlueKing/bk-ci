package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class License(
    val key: String,
    val name: String,
    @JsonProperty("node_id")
    val nodeId: String,
    @JsonProperty("spdx_id")
    val spdxId: String,
    val url: String?
)
