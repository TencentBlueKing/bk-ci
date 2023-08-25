package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData

@JsonIgnoreProperties(ignoreUnknown = true)
data class CgsResourceConfig(
    @JsonProperty("zoneId")
    val zoneId: List<String>,
    @JsonProperty("machineType")
    val machineType: List<String>
)
