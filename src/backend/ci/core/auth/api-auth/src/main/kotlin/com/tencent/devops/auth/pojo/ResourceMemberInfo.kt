package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "成员信息")
data class ResourceMemberInfo(
    @get:Schema(title = "成员id")
    val id: String,
    @get:Schema(title = "成员名称")
    val name: String? = null,
    @get:Schema(title = "成员类型")
    val type: String,
    @get:Schema(title = "是否离职")
    val departed: Boolean? = false
)
