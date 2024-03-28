package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class CmdbKeyValues(
    @get:Schema(title = "主机ip")
    @JsonProperty("SvrIp")
    val svrIp: String? = ""
)