package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线可见范围")
data class PipelineVisibility(
    @get:Schema(title = "可见范围类型", required = true)
    val type: PipelineVisibilityType,
    @get:Schema(title = "范围ID", required = true)
    val scopeId: String,
    @get:Schema(title = "范围标识名", required = true)
    val scopeName: String,
    @get:Schema(title = "完整名称", required = true)
    val fullName: String,
    @get:Schema(title = "用户部门信息", required = true)
    val userDepartments: List<String>? = emptyList()
)
