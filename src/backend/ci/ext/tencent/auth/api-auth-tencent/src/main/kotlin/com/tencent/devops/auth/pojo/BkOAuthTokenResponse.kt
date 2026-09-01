package com.tencent.devops.auth.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "bkoauth接口响应外层结构")
data class BkOAuthTokenResponse(
    @get:Schema(title = "请求结果")
    val result: Boolean,
    @get:Schema(title = "返回码")
    val code: String,
    @get:Schema(title = "返回信息")
    val message: String?,
    @get:Schema(title = "返回数据")
    val data: BkOAuthTokenData?
)

@Schema(title = "bkoauth接口响应数据")
data class BkOAuthTokenData(
    @get:Schema(title = "用户ID")
    @JsonProperty("user_id")
    val userId: String? = null,
    @get:Schema(title = "access_token")
    @JsonProperty("access_token")
    val accessToken: String,
    @get:Schema(title = "有效时间(秒)")
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @get:Schema(title = "用户类型")
    @JsonProperty("user_type")
    val userType: String? = null,
    @get:Schema(title = "授权范围")
    val scope: String? = null,
    @get:Schema(title = "refresh_token")
    @JsonProperty("refresh_token")
    val refreshToken: String? = null
)
