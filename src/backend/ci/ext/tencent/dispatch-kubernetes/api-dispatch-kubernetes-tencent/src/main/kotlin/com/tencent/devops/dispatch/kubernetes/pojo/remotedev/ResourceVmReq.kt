package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmReq(
    val zoneId: String,
    val machineType: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmResp(
    val result: Boolean,
    val code: Int,
    val message: String?,
    val data: ResourceVmRespData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ResourceVmRespData(
    val cap: Int,
    val used: Int,
    val free: Int,
    val machineType: String,
    val zoneId: String
)