package com.tencent.devops.repository.pojo

data class RepositoryGitCheck(
    val pipelineId: String,
    val buildNumber: Int,
    val repositoryId: String?,
    val repositoryName: String?,
    val commitId: String,
    val context: String,
    val source: ExecuteSource
)
