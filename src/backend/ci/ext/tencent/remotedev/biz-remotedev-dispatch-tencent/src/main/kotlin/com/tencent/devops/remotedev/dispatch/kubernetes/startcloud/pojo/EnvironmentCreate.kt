package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.remotedev.pojo.Pvc

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
    @JsonProperty("gameId")
    val gameId: String,
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
    val image: String? = "",
    @JsonProperty("internal")
    val internal: Boolean = false,
    @JsonProperty("pvcs")
    val pvcs: List<Pvc> = emptyList(),
    val tolerations: List<Toleration>? = null,
    val nodeSelector: Map<String, String>? = null
) {
    data class Toleration(
        val key: String = "bkbcs.tencent.com/node-group",
        val operator: String = "Equal",
        val value: String,
        val effect: String = "NoSchedule"
    )
}
