package com.tencent.devops.environment.pojo.thirdPartyAgent

data class HeartbeatResponse(
    val masterVersion: String,
    val slaveVersion: String,
    val AgentStatus: String,
    val ParallelTaskCount: Int,
    val envs: Map<String, String>
)