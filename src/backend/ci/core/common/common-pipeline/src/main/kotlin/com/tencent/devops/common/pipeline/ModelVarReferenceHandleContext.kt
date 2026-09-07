package com.tencent.devops.common.pipeline

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Model变量引用处理上下文")
data class ModelVarReferenceHandleContext(
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "模型对象", required = false)
    val model: Model? = null,
    @get:Schema(title = "资源版本", required = true)
    val resourceVersion: Int
)
