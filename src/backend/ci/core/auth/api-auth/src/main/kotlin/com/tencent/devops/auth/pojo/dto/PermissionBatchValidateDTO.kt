package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "权限批量校验实体")
data class PermissionBatchValidateDTO(
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源code", required = true)
    val resourceCode: String,
    @get:Schema(title = "action类型列表", required = true)
    val actionList: List<String>
)
