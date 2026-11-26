package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "PAC 流水线变更后刷新流水线组请求")
data class PipelineYamlViewGroupUpdateReq(
    @get:Schema(title = "流水线 ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(title = "创建人", required = true)
    val creator: String
)
