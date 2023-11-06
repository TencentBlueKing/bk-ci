package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCHostPropertyFilter<T>(
    @ApiModelProperty(value = "组合查询条件")
    @JsonProperty("condition")
    val condition: String?,
    @ApiModelProperty(value = "规则")
    @JsonProperty("rules")
    val rules: List<CCRules<T>>?
)