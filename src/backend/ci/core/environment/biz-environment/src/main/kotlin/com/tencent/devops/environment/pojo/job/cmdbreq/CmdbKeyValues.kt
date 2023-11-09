package com.tencent.devops.environment.pojo.job.cmdbreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class CmdbKeyValues(
    @ApiModelProperty(value = "主机ip")
    @JsonProperty("SvrIp")
    val svrIp: String? = ""
)