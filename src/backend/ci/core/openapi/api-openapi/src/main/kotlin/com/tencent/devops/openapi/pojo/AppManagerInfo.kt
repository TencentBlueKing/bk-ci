package com.tencent.devops.openapi.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "")
data class AppManagerInfo(
    @get:Schema(title = "app code")
    val appCode: String,
    @get:Schema(title = "管理员")
    val managerUser: String
)
