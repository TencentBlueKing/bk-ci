package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCPage(
    @get:Schema(title = "记录开始位置", required = true)
    @JsonProperty("start")
    val start: Int?,
    @get:Schema(title = "每页限制条数，最大500", required = true)
    @JsonProperty("limit")
    val limit: Int?
)