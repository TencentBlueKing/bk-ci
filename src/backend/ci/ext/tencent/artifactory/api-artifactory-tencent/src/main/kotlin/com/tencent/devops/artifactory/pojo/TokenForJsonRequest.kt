package com.tencent.devops.artifactory.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "申请临时token给JSON保存")
data class TokenForJsonRequest(
    @get:Schema(title = "JSON", required = true)
    val json: String,
    @get:Schema(title = "过期秒数", required = true)
    val ttlSecond: Int
)
