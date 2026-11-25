package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线操作人")
data class PipelineOperator(
    @get:Schema(title = "用户ID", required = true)
    val userId: String,
    @get:Schema(title = "用户名称", required = true)
    val userName: String
)
