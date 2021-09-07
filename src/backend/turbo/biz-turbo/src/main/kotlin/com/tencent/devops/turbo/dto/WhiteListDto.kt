package com.tencent.devops.turbo.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class WhiteListDto(
    val ip: String,
    @JsonProperty("project_id")
    val projectId: String,
    val message: String? = ""
)
