package com.tencent.devops.auth.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "oauth2获取token中间处理态")
data class Oauth2AccessTokenDTO(
    @get:Schema(title = "accessToken", required = true)
    val accessToken: String? = null,
    @get:Schema(title = "refreshToken", required = true)
    val refreshToken: String? = null,
    @get:Schema(title = "accessToken过期时间", required = true)
    val expiredTime: Long? = null,
    @get:Schema(title = "accessToken绑定的用户名称", required = true)
    val userName: String? = null,
    @get:Schema(title = "accessToken绑定的密码", required = true)
    val passWord: String? = null,
    @get:Schema(title = "授权范围Id", required = true)
    val scopeId: Int
)
