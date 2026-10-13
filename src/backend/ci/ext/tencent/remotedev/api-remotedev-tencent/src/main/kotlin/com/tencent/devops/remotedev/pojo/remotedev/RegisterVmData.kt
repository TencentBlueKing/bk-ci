package com.tencent.devops.remotedev.pojo.remotedev

/**
 * BCS 注册第三方机器为云桌面请求体 (POST /api/v1/remotedevenv/registervm)
 */
data class RegisterVmData(
    val internal: Boolean,
    val ip: String,
    val os: String,
    val machineType: String,
    val zoneId: String,
    val skipInit: Boolean
)
