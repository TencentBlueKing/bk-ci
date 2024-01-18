package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "默认组信息")
data class DefaultGroup(
    @get:Schema(title = "名称")
    val name: String,
    @get:Schema(title = "展示名称")
    val displayName: String,
    @get:Schema(title = "组编码")
    val code: String
)
