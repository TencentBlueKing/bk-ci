package com.tencent.devops.environment.pojo.thirdpartyagent

data class RegistryResp(
    val gateway: String,
    val fileGateway: String,
    val projectId: String,
    val agentId: String,
    val secretKey: String,
    val parallelTaskCount: Int,
    val dockerParallelTaskCount: Int,
    val language: String
)
