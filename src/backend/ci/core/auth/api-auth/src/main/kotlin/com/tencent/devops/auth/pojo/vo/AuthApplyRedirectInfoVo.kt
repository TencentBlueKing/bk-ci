package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "弹框跳转信息")
data class AuthApplyRedirectInfoVo(
    @Schema(name = "是否开启权限")
    val auth: Boolean,
    @Schema(name = "资源类型名称")
    val resourceTypeName: String,
    @Schema(name = "资源实例名称")
    val resourceName: String,
    @Schema(name = "动作名称")
    val actionName: String? = null,
    @Schema(name = "用户组信息列表")
    val groupInfoList: List<AuthRedirectGroupInfoVo>
)
