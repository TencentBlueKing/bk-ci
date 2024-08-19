package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户有权限的用户组数量")
data class MemberGroupCountWithPermissionsVo(
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源类型名")
    val resourceTypeName: String,
    @get:Schema(title = "数量")
    val count: Long
)
