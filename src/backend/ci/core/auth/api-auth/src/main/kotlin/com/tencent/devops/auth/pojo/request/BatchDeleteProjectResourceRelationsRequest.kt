package com.tencent.devops.auth.pojo.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量删除多个项目资源关系请求")
data class BatchDeleteProjectResourceRelationsRequest(
    @get:Schema(title = "项目Code列表", required = true)
    val projectCodes: List<String>,
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "是否仅预演，不执行真实删除", required = true)
    val dryRun: Boolean = true,
    @get:Schema(title = "是否确认执行删除，仅在 dryRun=false 时生效", required = true)
    val confirm: Boolean = false
)
