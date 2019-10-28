package com.tencent.devops.common.environment.agent.pojo.agent

data class RawCcNode(
    val name: String,
    val assetID: String,
    val operator: String,
    val bakOperator: String,
    var ip: String,
    val displayIp: String,
    val osName: String,
    var agentStatus: Boolean
)