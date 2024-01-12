package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "弹框跳转信息")
data class AuthApplyRedirectInfoVo(
    @Schema(description = "是否开启权限")
    val auth: Boolean,
    @Schema(description = "资源类型名称")
    val resourceTypeName: String,
    @Schema(description = "资源实例名称")
    val resourceName: String,
    @Schema(description = "动作名称")
    val actionName: String? = null,
    @Schema(description = "用户组信息列表")
    val groupInfoList: List<AuthRedirectGroupInfoVo>
)
