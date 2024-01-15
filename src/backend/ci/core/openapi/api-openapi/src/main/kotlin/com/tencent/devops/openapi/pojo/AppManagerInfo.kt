package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "")
data class AppManagerInfo(
    @Schema(name = "app code")
    val appCode: String,
    @Schema(name = "管理员")
    val managerUser: String
)
