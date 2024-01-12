package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "默认组信息")
data class DefaultGroup(
    @Schema(description = "名称")
    val name: String,
    @Schema(description = "展示名称")
    val displayName: String,
    @Schema(description = "组编码")
    val code: String
)
