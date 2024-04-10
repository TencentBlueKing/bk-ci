package com.tencent.devops.lambda.pojo

data class DataPlatBuildCommits(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val commitId: String,
    val authorName: String,
    val message: String,
    val repoType: String,
    val commitTime: String,
    val createTime: String,
    val mrId: String,
    val url: String,
    val eventType: String,
    val channel: String,
    val action: String?
)
