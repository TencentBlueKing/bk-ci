package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "鉴权记录实体")
data class VerifyRecordDTO(
    @Schema(description = "用户ID")
    val userId: String,
    @Schema(description = "项目ID")
    val projectId: String,
    @Schema(description = "资源类型")
    val resourceType: String,
    @Schema(description = "资源Code")
    val resourceCode: String,
    @Schema(description = "操作")
    val action: String,
    @Schema(description = "鉴权结果")
    val verifyResult: Boolean
)
