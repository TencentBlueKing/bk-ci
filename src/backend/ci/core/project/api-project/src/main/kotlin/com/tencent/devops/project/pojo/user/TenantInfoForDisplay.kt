package com.tencent.devops.project.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "租户信息(用于展示)")
data class TenantInfoForDisplay(
    @get:Schema(title = "租户ID")
    val tenantId: String,
    @get:Schema(title = "依赖API")
    val apiBaseUrl: String
)
