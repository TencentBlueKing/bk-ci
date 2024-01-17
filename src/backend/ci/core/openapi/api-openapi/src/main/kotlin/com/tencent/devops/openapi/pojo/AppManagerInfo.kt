package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "")
data class AppManagerInfo(
    @Schema(title = "app code")
    val appCode: String,
    @Schema(title = "管理员")
    val managerUser: String
)
