package com.tencent.devops.environment.pojo.thirdpartyagent

data class AgentShared(
    val agentId: Long,
    val mainProjectId: String,
    val sharedProjectId: List<String>,
    val userId: String
)
