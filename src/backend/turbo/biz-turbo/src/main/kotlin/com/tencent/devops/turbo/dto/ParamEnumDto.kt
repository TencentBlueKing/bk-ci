package com.tencent.devops.turbo.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ParamEnumDto(
    @JsonProperty("param_value")
    val paramValue: Any,
    @JsonProperty("param_name")
    val paramName: String,
    @JsonProperty("visual_range")
    val visualRange: List<String> = listOf()
)
