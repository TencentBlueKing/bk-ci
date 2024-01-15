package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Oauth2授权操作信息请求实体")
data class ScopeOperationDTO(
    @Schema(name = "主键ID")
    val id: Int,
    @Schema(name = "授权操作ID")
    val operationId: String,
    @Schema(name = "授权操作中文名称")
    val operationNameCn: String,
    @Schema(name = "授权操作英文名称")
    val operationNameEn: String
)
