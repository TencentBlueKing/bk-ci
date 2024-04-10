package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class QueryCCListHostWithoutBizData(
    @get:Schema(title = "记录条数", required = true)
    @JsonProperty("count")
    val count: Int,
    @get:Schema(title = "主机实际数据", required = true)
    @JsonProperty("info")
    val info: List<CCInfo>
)