package com.tencent.devops.dockerhost.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class LeakScanResponse(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: String,
    @JsonProperty("request_id")
    val requestId: String
)
