package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "成员信息")
data class MemberInfo(
    @Schema(name = "成员id")
    val id: String,
    @Schema(name = "成员名称")
    val name: String,
    @Schema(name = "成员类别")
    val type: String
)
