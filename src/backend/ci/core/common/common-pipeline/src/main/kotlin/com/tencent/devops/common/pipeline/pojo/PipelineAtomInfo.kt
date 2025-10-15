package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class PipelineAtomInfo (
    @Schema(title = "项目id")
    val projectId:String,
    @Schema(title = "流水线id")
    val pipelineId:String,
    @Schema(title = "插件版本号")
    val versions:String,
)