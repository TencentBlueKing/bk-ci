package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "PAC 流水线组清理请求")
data class PipelineYamlViewCleanupReq(
    @get:Schema(title = "代码库 hashId", required = true)
    val repoHashId: String,
    @get:Schema(title = "目录", required = true)
    val directory: String,
    @get:Schema(title = "流水线 ID", required = true)
    val pipelineId: String
)
