package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "请求返回实体")
data class ResponseDTO<T>(
    @get:Schema(title = "返回码")
    val code: Long,
    @get:Schema(title = "返回信息")
    val message: String,
    @get:Schema(title = "请求返回结果")
    val result: Boolean,
    @get:Schema(title = "请求返回数据")
    val data: T?
)
