package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "鉴权记录实体")
data class VerifyRecordDTO(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "资源类型")
    val resourceType: String,
    @get:Schema(title = "资源Code")
    val resourceCode: String,
    @get:Schema(title = "操作")
    val action: String,
    @get:Schema(title = "鉴权结果")
    val verifyResult: Boolean
)
