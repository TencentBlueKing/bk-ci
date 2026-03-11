package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCHostPropertyFilter<T>(
    @get:Schema(title = "组合查询条件")
    @JsonProperty("condition")
    val condition: String?,
    @get:Schema(title = "规则")
    @JsonProperty("rules")
    val rules: List<CCRules<T>>?
)