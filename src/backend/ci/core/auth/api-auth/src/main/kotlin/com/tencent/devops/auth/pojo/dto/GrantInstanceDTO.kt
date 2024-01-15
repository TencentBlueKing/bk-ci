package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "实例授权入参")
data class GrantInstanceDTO(
    @Schema(name = "资源类型, 如：pipeline,project等")
    val resourceType: String,
    @Schema(name = "资源实例编码, 如：pipelineId,projectId等")
    val resourceCode: String,
    @Schema(name = "资源名称")
    val resourceName: String?,
    @Schema(name = "权限类型, 如：create,edit等")
    val permission: String,
    @Schema(name = "目标操作用户")
    val createUser: String
)
