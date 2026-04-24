package com.tencent.devops.remotedev.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "日志上传COS签名地址")
data class LogUploadUrl(
    @get:Schema(description = "签名后的上传地址")
    val url: String,
    @get:Schema(description = "签名有效期(秒)")
    val expireSeconds: Long
)
