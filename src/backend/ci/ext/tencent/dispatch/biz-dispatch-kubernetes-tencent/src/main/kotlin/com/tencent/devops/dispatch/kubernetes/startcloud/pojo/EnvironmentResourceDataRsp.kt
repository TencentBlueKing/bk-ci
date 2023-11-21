package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.EnvironmentResourceData

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentResourceDataRsp(
    val code: Int,
    val data: DataRsp?,
    val message: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DataRsp(
    @JsonProperty("rows")
    val rows: List<EnvironmentResourceData>,
    @JsonProperty("total")
    val total: Int
)
