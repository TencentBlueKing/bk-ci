package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "弹框跳转信息")
data class AuthApplyRedirectInfoVo(
    @get:Schema(title = "是否开启权限")
    val auth: Boolean,
    @get:Schema(title = "资源类型名称")
    val resourceTypeName: String,
    @get:Schema(title = "资源实例名称")
    val resourceName: String,
    @get:Schema(title = "动作名称")
    val actionName: String? = null,
    @get:Schema(title = "用户组信息列表")
    val groupInfoList: List<AuthRedirectGroupInfoVo>,
    @get:Schema(title = "管理员列表")
    val managers: List<String> = emptyList()
)
