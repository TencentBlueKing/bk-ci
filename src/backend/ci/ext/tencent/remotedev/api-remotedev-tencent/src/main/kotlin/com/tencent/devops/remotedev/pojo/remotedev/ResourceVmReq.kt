package com.tencent.devops.remotedev.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmReq(
    val zoneId: String?,
    val machineType: String?,
    val internal: Boolean?,
    val tolerations: List<Toleration>? = null,
    val nodeSelector: Map<String, String>? = null
) {
    data class Toleration(
        val key: String = "bkbcs.tencent.com/node-group",
        val operator: String = "Equal",
        val value: String,
        val effect: String = "NoSchedule"
    )

    constructor(
        zoneId: String?,
        machineType: String?,
        internal: Boolean?,
        specifyTaints: String? = null
    ) : this(
        zoneId = zoneId,
        machineType = machineType,
        internal = internal,
        tolerations = if (specifyTaints != null) {
            listOf(Toleration(value = specifyTaints))
        } else null,
        nodeSelector = if (specifyTaints != null) {
            mapOf("bkbcs.tencent.com/node-group" to specifyTaints)
        } else null
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmResp(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: ResourceVmRespResource?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmRespResource(
    val zoneResources: List<ResourceVmRespData>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmRespData(
    val machineResources: List<ResourceVmRespDataMachineResource>?,
    val zoneId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmRespDataMachineResource(
    val cap: Int?,
    val used: Int?,
    val free: Int?,
    val machineType: String,
    val zoneId: String?
)
