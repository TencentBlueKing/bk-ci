package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckRunOutput(
    @JsonProperty("annotations_count")
    val annotationsCount: Int,
    @JsonProperty("annotations_url")
    val annotationsUrl: String,
    val summary: String?,
    val text: String?,
    val title: String?
)
