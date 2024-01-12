package com.tencent.devops.notify.tencentcloud.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("LongParameterList")
@Schema(description = "腾讯云签名配置信息")
open class TencentCloudSignatureConfig(
    @Schema(description = "接口请求方法")
    val httpRequestMethod: String,
    @Schema(description = "服务标识")
    val service: String,
    @Schema(description = "host地址")
    val host: String,
    @Schema(description = "请求地址")
    val url: String,
    @Schema(description = "操作的接口名称")
    val action: String,
    @Schema(description = "操作的 API 的版本")
    val version: String,
    @Schema(description = "签名方法，目前固定取该值TC3-HMAC-SHA256")
    val algorithm: String = "TC3-HMAC-SHA256",
    @Schema(description = "地域参数，用来标识希望操作哪个地域的数据")
    open val region: String,
    @Schema(description = "请求体")
    open val payload: String,
    @Schema(description = "密钥id")
    open val secretId: String,
    @Schema(description = "密钥key")
    open val secretKey: String
)
