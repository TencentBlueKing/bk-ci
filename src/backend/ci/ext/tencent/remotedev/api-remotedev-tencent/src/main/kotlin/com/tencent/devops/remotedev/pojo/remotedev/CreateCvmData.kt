package com.tencent.devops.remotedev.pojo.remotedev

data class CreateCvmData(
    val createOnly: Boolean?,
    val internal: Boolean,
    val ip: String,
    val machineType: String,
    val projectId: String?,
    val userId: String?,
    val zoneId: String
)

data class CreateCvmResp(
    val environmentUid: String?,
    val taskID: String,
    val taskUid: String
)
