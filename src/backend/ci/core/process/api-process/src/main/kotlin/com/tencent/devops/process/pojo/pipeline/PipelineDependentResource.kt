package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线依赖资源")
data class PipelineDependentResource(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(title = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(title = "资源名称", required = true)
    val resourceName: String
)
