package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty

import io.swagger.v3.oas.annotations.media.Schema

data class CCRules<T>(
    @get:Schema(title = "字段名", required = true)
    @JsonProperty("field")
    val field: String,
    @get:Schema(
        title = "操作符", description = "可选值:equal,not_equal,in,not_in,less,less_or_equal," +
        "greater,greater_or_equal,between,not_between", required = true
    )
    @JsonProperty("operator")
    val operator: String,
    @get:Schema(title = "操作数", description = "不同的operator对应不同的value格式", required = true)
    @JsonProperty("value")
    val value: T?
)