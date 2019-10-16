package com.tencent.devops.common.misc.pojo.agent

data class BcsVmNode(
    val name: String,
    val clusterId: String,
    val namespace: String,
    val osName: String,
    val ip: String,
    var status: String
)
