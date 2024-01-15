package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "申请加入用户组itsm表单内容")
data class ApplyJoinGroupFormDataInfo(
    @Schema(name = "项目名称")
    val projectName: String,
    @Schema(name = "资源名称类型")
    val resourceTypeName: String,
    @Schema(name = "资源名称")
    val resourceName: String,
    @Schema(name = "用户组名称")
    val groupName: String,
    @Schema(name = "申请期限")
    val validityPeriod: String,
    @Schema(name = "资源跳转链接")
    val resourceRedirectUri: String? = null,
    @Schema(name = "用户组权限详情跳转链接")
    val groupPermissionDetailRedirectUri: String? = null
)
