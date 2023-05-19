package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentCreate(
    @JsonProperty("UserID")
    val userId: String,
    @JsonProperty("Ticket")
    val ticket: String?,
    @JsonProperty("AppName")
    val appName: String,
    @JsonProperty("PipeLineID")
    val pipeLineId: String?
)
