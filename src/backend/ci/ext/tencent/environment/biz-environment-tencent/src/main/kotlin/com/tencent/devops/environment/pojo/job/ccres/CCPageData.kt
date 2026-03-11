package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCPageData<T>(
    @get:Schema(title = "记录条数", required = true)
    @JsonProperty("count")
    val count: Int,
    @get:Schema(title = "实际数据", required = true)
    @JsonProperty("info")
    val info: List<T>
)
