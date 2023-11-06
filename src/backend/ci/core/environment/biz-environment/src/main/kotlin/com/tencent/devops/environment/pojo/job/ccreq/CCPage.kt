package com.tencent.devops.environment.pojo.job.ccreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CCPage(
    @ApiModelProperty(value = "记录开始位置", required = true)
    @JsonProperty("start")
    val start: Int?,
    @ApiModelProperty(value = "每页限制条数，最大500", required = true)
    @JsonProperty("limit")
    val limit: Int?
)