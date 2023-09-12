package com.tencent.devops.environment.pojo.thirdPartyAgent

data class AgentShared(
    val agentId: Long,
    val mainProjectId: String,
    val sharedProjectId: List<String>,
    val userId: String
)
