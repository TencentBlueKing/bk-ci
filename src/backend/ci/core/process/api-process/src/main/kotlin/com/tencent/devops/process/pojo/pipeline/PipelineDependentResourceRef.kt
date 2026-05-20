package com.tencent.devops.process.pojo.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线依赖资源引用")
data class PipelineDependentResourceRef(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "引用类型", required = true)
    val refType: ReferenceType,
    @get:Schema(title = "引用值", required = true)
    val refValue: String
)

@Schema(title = "资源引用类型")
enum class ReferenceType {
    ID,
    NAME
}
