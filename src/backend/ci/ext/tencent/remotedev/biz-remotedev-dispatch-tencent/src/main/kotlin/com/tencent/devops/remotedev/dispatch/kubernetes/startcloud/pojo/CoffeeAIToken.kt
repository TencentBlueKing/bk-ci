package com.tencent.devops.remotedev.dispatch.kubernetes.startcloud.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * token注册信息
 */
@Schema(title = "token注册信息")
data class CoffeeAIToken(
    @get:Schema(title = "用户id", required = true)
    @JsonProperty("userId")
    val userId: String,

    @get:Schema(title = "token", required = true)
    @JsonProperty("token")
    val token: String,

    @get:Schema(title = "过期时间", required = true)
    @JsonProperty("expirationMinutes")
    val expirationMinutes: Long
)
