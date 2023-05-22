package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class EnvironmentDelete(
    @JsonProperty("UserID")
    val userId: String,
    @JsonProperty("AppName")
    val appName: String,
    @JsonProperty("Ticket")
    val ticket: String?,
    @JsonProperty("PipeLineID")
    val pipeLineId: String?
)
