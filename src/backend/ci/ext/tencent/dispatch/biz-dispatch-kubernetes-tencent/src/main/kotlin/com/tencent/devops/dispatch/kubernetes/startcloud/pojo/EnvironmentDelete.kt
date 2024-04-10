package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentDelete(
    @JsonProperty("UserID")
    val userId: String,
    @JsonProperty("AppName")
    val appName: String,
    @JsonProperty("PipeLineID")
    val pipeLineId: String?
)
