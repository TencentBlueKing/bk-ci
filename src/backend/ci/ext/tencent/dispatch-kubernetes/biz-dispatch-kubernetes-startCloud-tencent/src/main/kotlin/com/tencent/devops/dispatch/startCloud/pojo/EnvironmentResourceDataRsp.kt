package com.tencent.devops.dispatch.startCloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EnvironmentResourceDataRsp(
    val code: Int,
    val data: List<EnvironmentResourceData>?,
    val message: String
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class EnvironmentResourceData(
        @JsonProperty("cgs_ip")
        val cgsIp: String,
        @JsonProperty("zone_id")
        val zoneId: String,
        @JsonProperty("status")
        val status: Int
    )
}
