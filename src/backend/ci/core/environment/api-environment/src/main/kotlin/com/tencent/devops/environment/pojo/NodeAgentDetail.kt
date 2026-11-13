package com.tencent.devops.environment.pojo

data class NodeAgentDetail(
    val agentHashId: String,
    val displayName: String,
    val ip: String,
    val workspaceName: String,
    val nodeId: Long,
    val os: String? = null
)
