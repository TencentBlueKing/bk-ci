package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class EnvironmentResourceData(
    @JsonProperty("cgsId")
    val cgsId: String,
    @JsonProperty("cgsIp")
    val cgsIp: String,
    @JsonProperty("zoneId")
    val zoneId: String,
    @JsonProperty("machineType")
    val machineType: String,
    @JsonProperty("status")
    val status: Int,
    @JsonProperty("userInstanceList")
    val userInstanceList: List<Instance>? = null,
    var locked: Boolean? = false,
    val projectId: String?,
    val disk: String?,
    val hdisk: String?,
    var imageStandard: Boolean? = true,
    val node: String?,
    val image: String?,
    val cpu: String?,
    val mem: String?,
    val registerCgsTime: LocalDateTime?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Instance(
    @JsonProperty("userId")
    val userId: String,
    @JsonProperty("status")
    val status: Int,
    @JsonProperty("pipelineId")
    val pipelineId: String
)
