package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class QueryCCListHostWithoutBizData(
    @ApiModelProperty(value = "记录条数", required = true)
    @JsonProperty("count")
    val count: Int,
    @ApiModelProperty(value = "主机实际数据", required = true)
    @JsonProperty("info")
    val info: List<CCInfo>
)