package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "资源组详情")
data class ResourceGroupInfo(
    @Schema(name = "用户组ID")
    val groupId: String,
    @Schema(name = "用户组名称")
    val groupName: String,
    @Schema(name = "项目code")
    val projectCode: String,
    @Schema(name = "资源类型")
    val resourceType: String,
    @Schema(name = "资源名称")
    val resourceName: String,
    @Schema(name = "资源code")
    val resourceCode: String
)
