package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.enum.PermissionTagType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限推荐/警告标签")
data class PermissionTagVO(
    @get:Schema(title = "标签类型", required = true)
    val type: PermissionTagType,
    @get:Schema(title = "标签文案", required = true)
    val text: String
)
