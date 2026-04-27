package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "蓝鲸平台access_token信息")
data class BkAccessTokenInfo(
    @get:Schema(title = "access_token", required = true)
    val accessToken: String,
    @get:Schema(title = "过期时间戳(秒)", required = true)
    val expiredTime: Long,
    @get:Schema(title = "用户ID", required = true)
    val userId: String
)
