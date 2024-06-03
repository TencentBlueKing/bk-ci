package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限申请跳转-用户组信息")
data class AuthRedirectGroupInfoVo(
    @get:Schema(title = "跳转URL")
    val url: String,
    @get:Schema(title = "用户组名")
    val groupName: String? = null
)
