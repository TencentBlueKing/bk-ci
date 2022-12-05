package com.tencent.devops.process.pojo.code

import io.swagger.annotations.ApiModel
import java.time.LocalDateTime

@ApiModel("构建提交信息")
data class PipelineBuildCommit(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val commitId: String,
    val authorName: String,
    val message: String,
    val repoType: String,
    val commitTime: LocalDateTime,
    val url: String,
    val eventType: String,
    val mrId: String?,
    val channel: String,
    val action: String?
)
