package com.tencent.devops.common.misc.pojo.agent

data class RawCmdbNode(
    val name: String,
    val operator: String,
    val bakOperator: String,
    var ip: String,
    val displayIp: String,
    val osName: String,
    var agentStatus: Boolean
)
