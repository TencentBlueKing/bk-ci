package com.devops.process.yaml.v2.stageCheck

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReviewVariable(
    val label: String?,
    val type: String,
    val default: Any?,
    val values: Any?,
    val description: String?
)
