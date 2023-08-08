package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentResourceData(
    @JsonProperty("cgs_ip")
    val cgsIp: String,
    @JsonProperty("zone_id")
    val zoneId: String,
    @JsonProperty("status")
    val status: Int
)
