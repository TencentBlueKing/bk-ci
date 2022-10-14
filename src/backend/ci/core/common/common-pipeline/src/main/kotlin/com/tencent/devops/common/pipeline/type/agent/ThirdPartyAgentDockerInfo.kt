package com.tencent.devops.common.pipeline.type.agent

data class ThirdPartyAgentDockerInfo(
    val image: String,
    val credential: Credential?,
    val envs: Map<String, String>?
)

data class Credential(
    val user: String,
    val password: String,
)
