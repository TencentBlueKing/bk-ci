package com.tencent.devops.plugin.codecc.pojo.coverity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoverityResult(
    val status: Int = 0,
    val code: String = "",
    val message: String? = "no need register",
    val data: Any? = true
)