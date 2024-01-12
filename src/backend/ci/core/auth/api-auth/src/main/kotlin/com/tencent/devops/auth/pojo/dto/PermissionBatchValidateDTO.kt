package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "权限批量校验实体")
data class PermissionBatchValidateDTO(
    @Schema(description = "资源类型", required = true)
    val resourceType: String,
    @Schema(description = "资源code", required = true)
    val resourceCode: String,
    @Schema(description = "action类型列表", required = true)
    val actionList: List<String>
)
