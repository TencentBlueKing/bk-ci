package com.tencent.devops.dispatch.devcloud.pojo.devcloud

data class ListPerformancesReq(
    val t1: String,
    val t2: String,
    val t3: String,
    val username: String,
    val rsType: String
)

enum class RsType(val value: String) {
    DOCKER("docker"),
    MAC("mac"),
    WINDOWS("windows")
}