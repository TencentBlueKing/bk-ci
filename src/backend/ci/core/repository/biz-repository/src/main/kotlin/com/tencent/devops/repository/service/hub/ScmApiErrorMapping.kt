package com.tencent.devops.repository.service.hub

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Scm api错误码映射")
data class ScmApiErrorMapping(
    @get:Schema(title = "http状态码", required = true)
    val httpStatus: Int,
    @get:Schema(title = "错误码", required = true)
    val errorCode: String,
    @get:Schema(title = "错误参数", required = false)
    val params: List<String>? = null
)
