package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Oauth2授权操作信息请求实体")
data class ScopeOperationDTO(
    @get:Schema(title = "主键ID")
    val id: Int,
    @get:Schema(title = "授权操作ID")
    val operationId: String,
    @get:Schema(title = "授权操作中文名称")
    val operationNameCn: String,
    @get:Schema(title = "授权操作英文名称")
    val operationNameEn: String
)
