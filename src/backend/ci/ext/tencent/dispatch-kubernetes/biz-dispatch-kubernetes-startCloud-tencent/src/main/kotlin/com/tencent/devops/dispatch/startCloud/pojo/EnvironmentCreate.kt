package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentCreate(
    @JsonProperty("userId")
    val userId: String,
    @JsonProperty("appName")
    val appName: String,
    @JsonProperty("pipeLineId")
    val pipeLineId: String?,
    @JsonProperty("zoneId")
    val zoneId: String?,
    @JsonProperty("machineType")
    val machineType: String?,
    @JsonProperty("ip")
    val cgsId: String? = ""
)
