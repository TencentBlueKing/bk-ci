package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "权限申请跳转-用户组信息")
data class AuthRedirectGroupInfoVo(
    @Schema(description = "跳转URL")
    val url: String,
    @Schema(description = "用户组名")
    val groupName: String? = null
)
