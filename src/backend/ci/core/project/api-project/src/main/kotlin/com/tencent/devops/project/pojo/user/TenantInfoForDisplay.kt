package com.tencent.devops.project.pojo.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "租户信息(用于展示)")
data class TenantInfoForDisplay(
    @get:Schema(title = "租户ID（企业空间）")
    val tenantId: String,
    @get:Schema(title = "依赖API")
    val apiBaseUrl: String,
    /**
     * IANA 时区，前端时间展示统一以此为准。
     * 暂默认 Asia/Shanghai（东八区），待蓝鲸用户管理 API 提供后改为真实值。
     */
    @get:Schema(title = "默认时区（IANA）")
    val timeZone: String = "Asia/Shanghai"
)
