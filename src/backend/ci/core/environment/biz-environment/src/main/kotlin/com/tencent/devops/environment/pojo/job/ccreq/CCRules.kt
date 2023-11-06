package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCRules<T>(
    @ApiModelProperty(value = "字段名", required = true)
    @JsonProperty("field")
    val field: String,
    @ApiModelProperty(
        value = "操作符", notes = "可选值:equal,not_equal,in,not_in,less,less_or_equal," +
        "greater,greater_or_equal,between,not_between", required = true
    )
    @JsonProperty("operator")
    val operator: String,
    @ApiModelProperty(value = "操作数", notes = "不同的operator对应不同的value格式", required = true)
    @JsonProperty("value")
    val value: T?
)