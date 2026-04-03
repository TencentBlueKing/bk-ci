package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线可见范围")
data class PipelineVisibility(
    @get:Schema(title = "可见范围类型", required = true)
    val type: PipelineVisibilityType,
    @get:Schema(title = "部门ID或用户ID", required = true)
    val scopeId: String,
    @get:Schema(title = "部门名称或用户名", required = true)
    val scopeName: String
)
