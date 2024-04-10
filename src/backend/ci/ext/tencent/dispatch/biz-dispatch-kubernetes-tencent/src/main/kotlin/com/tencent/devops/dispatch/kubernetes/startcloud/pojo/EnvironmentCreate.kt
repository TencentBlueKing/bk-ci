package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentCreate(
    @JsonProperty("basic")
    val basicBody: EnvironmentCreateBasicBody
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentCreateBasicBody(
    @JsonProperty("userId")
    val userId: String,
    @JsonProperty("appName")
    val appName: String,
    @JsonProperty("pipelineId")
    val pipelineId: String?,
    @JsonProperty("zoneId")
    val zoneId: String?,
    @JsonProperty("machineType")
    val machineType: String?,
    @JsonProperty("ip")
    val cgsId: String? = "",
    @JsonProperty("projectId")
    val projectId: String? = "",
    @JsonProperty("image")
    val image: String? = ""
)
