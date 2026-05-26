package com.tencent.devops.process.pojo.pipeline.task

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源基础信息")
data class PipelineCopyResourceBasicInfo(
    @get:Schema(description = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "资源名称", required = true)
    val resourceName: String
)
