package com.tencent.devops.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.Instance

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListCgsResp(
    val result: Boolean,
    val code: Int,
    val message: String,
    val data: List<ListCgsRespData>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListCgsRespData(
    val basic: ListCgsRespDataBasic?,
    val pvcs: List<ListCgsRespDataPvcsData>?,
    val image: String?,
    val cgsData: ListCgsRespDataCgsData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListCgsRespDataBasic(
    val appName: String?,
    val zoneId: String?,
    val machineType: String?,
    val projectId: String?,
    val ip: String?,
    val name: String?,
    val namespace: String?,
    val envId: String?,
    val status: String?,
    val user: String?,
    val clusterId: String?,
    val orderId: String?,
    val needLock: Boolean,
    val isCopying: Boolean?,
    val imageStandard: Boolean?,
    val node: String?,
    val image: String?,
    val cpuCores: Int?,
    val memoryLimit: String?,
    val registeCgsTime: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListCgsRespDataPvcsData(
    val pvcClass: String?,
    val pvcSize: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ListCgsRespDataCgsData(
    val cgsId: String,
    val cgsIp: String,
    val zoneId: String,
    val status: Int,
    val machineType: String,
    val userInstanceList: List<Instance>?,
    val envId: String?
)
