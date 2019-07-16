package com.tencent.devops.dispatch.pojo.thirdPartyAgent

data class AgentBuildInfo(
    val projectId: String,
    val agentId: String,
    val pipelineId: String,
    val pipelineName: String,
    val buildId: String,
    val buildNum: Int,
    val vmSeqId: String,
    val taskName: String,
    val status: String,
    val createdTime: Long,
    val updatedTime: Long,
    val workspace: String
)