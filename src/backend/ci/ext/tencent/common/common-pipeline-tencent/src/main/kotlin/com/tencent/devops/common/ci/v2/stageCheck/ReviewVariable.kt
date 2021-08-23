package com.tencent.devops.common.ci.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReviewVariable(
    val label: String?,
    val type: String,
    val default: Any?,
    val values: List<String>?,
    val description: String?
)
