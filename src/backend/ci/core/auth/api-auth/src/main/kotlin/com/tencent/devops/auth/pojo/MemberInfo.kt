package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "成员信息")
data class MemberInfo(
    @Schema(description = "成员id")
    val id: String,
    @Schema(description = "成员名称")
    val name: String,
    @Schema(description = "成员类别")
    val type: String
)
