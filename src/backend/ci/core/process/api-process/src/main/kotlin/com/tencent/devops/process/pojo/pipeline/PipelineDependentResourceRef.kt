package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceRefType
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线依赖资源引用")
data class PipelineDependentResourceRef(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(title = "引用类型", required = true)
    val refType: PipelineDependentResourceRefType,
    @get:Schema(title = "引用值", required = true)
    val refValue: String
)
