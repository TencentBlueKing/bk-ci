package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源类型权限概况")
data class ResourceSummaryVO(
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源类型名称", required = true)
    val resourceTypeName: String,
    @get:Schema(title = "用户组数量", required = true)
    val groupCount: Long,
    @get:Schema(title = "操作权限概要")
    val actionSummary: String? = null
)
