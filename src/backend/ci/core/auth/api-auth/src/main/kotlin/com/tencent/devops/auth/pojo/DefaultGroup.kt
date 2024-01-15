package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "默认组信息")
data class DefaultGroup(
    @Schema(name = "名称")
    val name: String,
    @Schema(name = "展示名称")
    val displayName: String,
    @Schema(name = "组编码")
    val code: String
)
