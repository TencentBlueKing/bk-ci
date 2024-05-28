package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "成员信息")
data class MemberInfo(
    @get:Schema(title = "成员id")
    val id: String,
    @get:Schema(title = "成员名称")
    val name: String,
    @get:Schema(title = "成员类别")
    val type: String
)
