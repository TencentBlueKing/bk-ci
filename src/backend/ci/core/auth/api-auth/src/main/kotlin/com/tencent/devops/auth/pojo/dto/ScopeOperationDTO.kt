package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Oauth2授权操作信息请求实体")
data class ScopeOperationDTO(
    @Schema(description = "主键ID")
    val id: Int,
    @Schema(description = "授权操作ID")
    val operationId: String,
    @Schema(description = "授权操作中文名称")
    val operationNameCn: String,
    @Schema(description = "授权操作英文名称")
    val operationNameEn: String
)
