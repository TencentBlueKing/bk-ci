package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "申请加入用户组itsm表单内容")
data class ApplyJoinGroupFormDataInfo(
    @Schema(description = "项目名称")
    val projectName: String,
    @Schema(description = "资源名称类型")
    val resourceTypeName: String,
    @Schema(description = "资源名称")
    val resourceName: String,
    @Schema(description = "用户组名称")
    val groupName: String,
    @Schema(description = "申请期限")
    val validityPeriod: String,
    @Schema(description = "资源跳转链接")
    val resourceRedirectUri: String? = null,
    @Schema(description = "用户组权限详情跳转链接")
    val groupPermissionDetailRedirectUri: String? = null
)
