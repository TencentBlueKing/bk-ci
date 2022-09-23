package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckRunAppPermissions(
    val contents: String,
    val issues: String,
    val metadata: String,
    @JsonProperty("single_file")
    val singleFile: String
)
