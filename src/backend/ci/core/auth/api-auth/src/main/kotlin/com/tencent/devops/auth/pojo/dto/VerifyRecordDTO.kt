package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "鉴权记录实体")
data class VerifyRecordDTO(
    @Schema(name = "用户ID")
    val userId: String,
    @Schema(name = "项目ID")
    val projectId: String,
    @Schema(name = "资源类型")
    val resourceType: String,
    @Schema(name = "资源Code")
    val resourceCode: String,
    @Schema(name = "操作")
    val action: String,
    @Schema(name = "鉴权结果")
    val verifyResult: Boolean
)
