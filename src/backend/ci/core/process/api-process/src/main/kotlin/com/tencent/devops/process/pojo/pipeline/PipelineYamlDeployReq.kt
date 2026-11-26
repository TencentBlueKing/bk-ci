package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "PAC yaml 文件变更部署请求")
data class PipelineYamlDeployReq(
    @get:Schema(title = "yaml 内容", required = true)
    val yaml: String,
    @get:Schema(title = "代码库 hashId", required = true)
    val repoHashId: String,
    @get:Schema(title = "文件路径", required = true)
    val filePath: String,
    @get:Schema(title = "分支", required = true)
    val ref: String,
    @get:Schema(title = "默认分支", required = true)
    val defaultBranch: String,
    @get:Schema(title = "提交信息", required = true)
    val commitMsg: String,
    @get:Schema(title = "合并请求 ID", required = false)
    val pullRequestId: Long? = null,
    @get:Schema(title = "合并请求 URL", required = false)
    val pullRequestUrl: String? = null
)
