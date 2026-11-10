package com.tencent.devops.repository.pojo.oauth

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库OAUTH授权状态")
data class ScmOauthStatus(
    @get:Schema(title = "是否已完成OAUTH授权", description = "true-已授权，false-未授权")
    val authorized: Boolean,
    @get:Schema(
        title = "OAUTH授权链接",
        description = "仅未授权时返回，用户打开该链接完成授权后，将跳转回redirectUrl；已授权时为空"
    )
    val url: String? = null
)
