package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "弹框跳转信息")
data class AuthApplyRedirectInfoVo(
    @Schema(title = "是否开启权限")
    val auth: Boolean,
    @Schema(title = "资源类型名称")
    val resourceTypeName: String,
    @Schema(title = "资源实例名称")
    val resourceName: String,
    @Schema(title = "动作名称")
    val actionName: String? = null,
    @Schema(title = "用户组信息列表")
    val groupInfoList: List<AuthRedirectGroupInfoVo>
)
