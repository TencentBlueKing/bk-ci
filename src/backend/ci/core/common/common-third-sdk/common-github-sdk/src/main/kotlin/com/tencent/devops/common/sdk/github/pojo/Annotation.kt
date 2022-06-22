package com.tencent.devops.common.sdk.github.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class Annotation(
    val path: String,
    @JsonProperty("start_line")
    val startLine: Int,
    @JsonProperty("end_line")
    val endLine: Int,
    @JsonProperty("start_column")
    val startColumn: Int?,
    @JsonProperty("end_column")
    val endColumn: Int?,
    @JsonProperty("annotation_level")
    val annotationLevel: String,
    val message: String,
    val title: String?,
    @JsonProperty("raw_details")
    val rawDetails: String?
)
