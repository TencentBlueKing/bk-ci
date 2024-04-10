package com.tencent.devops.environment.pojo.job.ccres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CCBkHost(
    @get:Schema(title = "CC中的机器host_id", required = true)
    @JsonProperty("bk_host_ids")
    val bkHostIds: List<Long>
)