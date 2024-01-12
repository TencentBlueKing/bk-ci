package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "资源组详情")
data class ResourceGroupInfo(
    @Schema(description = "用户组ID")
    val groupId: String,
    @Schema(description = "用户组名称")
    val groupName: String,
    @Schema(description = "项目code")
    val projectCode: String,
    @Schema(description = "资源类型")
    val resourceType: String,
    @Schema(description = "资源名称")
    val resourceName: String,
    @Schema(description = "资源code")
    val resourceCode: String
)
