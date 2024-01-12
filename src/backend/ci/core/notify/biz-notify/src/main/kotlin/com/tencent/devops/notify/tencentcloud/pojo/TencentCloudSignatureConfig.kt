package com.tencent.devops.notify.tencentcloud.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("LongParameterList")
@Schema(name = "腾讯云签名配置信息")
open class TencentCloudSignatureConfig(
    @Schema(name = "接口请求方法")
    val httpRequestMethod: String,
    @Schema(name = "服务标识")
    val service: String,
    @Schema(name = "host地址")
    val host: String,
    @Schema(name = "请求地址")
    val url: String,
    @Schema(name = "操作的接口名称")
    val action: String,
    @Schema(name = "操作的 API 的版本")
    val version: String,
    @Schema(name = "签名方法，目前固定取该值TC3-HMAC-SHA256")
    val algorithm: String = "TC3-HMAC-SHA256",
    @Schema(name = "地域参数，用来标识希望操作哪个地域的数据")
    open val region: String,
    @Schema(name = "请求体")
    open val payload: String,
    @Schema(name = "密钥id")
    open val secretId: String,
    @Schema(name = "密钥key")
    open val secretKey: String
)
