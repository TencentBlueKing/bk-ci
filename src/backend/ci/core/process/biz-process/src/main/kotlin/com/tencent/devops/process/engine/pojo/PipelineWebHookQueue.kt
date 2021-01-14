package com.tencent.devops.process.engine.pojo

data class PipelineWebHookQueue(
    val id: Long? = null,
    val pipelineId: String,
    val sourceProjectId: Long,
    val sourceRepoName: String,
    val sourceBranch: String,
    val targetProjectId: Long,
    val targetRepoName: String,
    val targetBranch: String,
    val buildId: String,
    val createTime: Long? = null
)