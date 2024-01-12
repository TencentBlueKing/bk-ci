package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "")
data class AppManagerInfo(
    @Schema(description = "app code")
    val appCode: String,
    @Schema(description = "管理员")
    val managerUser: String
)
