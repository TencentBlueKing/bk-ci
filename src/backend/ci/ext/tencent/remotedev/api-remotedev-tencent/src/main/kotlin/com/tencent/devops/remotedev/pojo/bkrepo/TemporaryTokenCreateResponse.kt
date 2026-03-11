package com.tencent.devops.remotedev.pojo.bkrepo

import io.swagger.v3.oas.annotations.media.Schema

/**
 * BkRepo临时Token创建响应
 */
@Schema(title = "BkRepo临时Token创建响应")
data class TemporaryTokenCreateResponse(
    @get:Schema(title = "临时访问Token", required = true)
    val token: String
)
