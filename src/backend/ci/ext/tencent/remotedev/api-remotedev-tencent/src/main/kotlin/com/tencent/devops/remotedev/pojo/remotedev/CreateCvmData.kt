package com.tencent.devops.remotedev.pojo.remotedev

data class CreateCvmData(
    val createOnly: Boolean?,
    val createType: String?,
    val internal: Boolean,
    val ip: String,
    val machineType: String,
    val projectId: String?,
    val userId: String?,
    val zoneId: String,
    val os: String? = null
)

data class CreateCvmResp(
    val environmentUid: String?,
    val taskID: String,
    val taskUid: String
)
