package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "资源组详情")
data class ResourceGroupInfo(
    @get:Schema(title = "用户组ID")
    val groupId: String,
    @get:Schema(title = "用户组名称")
    val groupName: String,
    @get:Schema(title = "项目code")
    val projectCode: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源名称")
    val resourceName: String,
    @get:Schema(title = "资源code")
    val resourceCode: String
)
