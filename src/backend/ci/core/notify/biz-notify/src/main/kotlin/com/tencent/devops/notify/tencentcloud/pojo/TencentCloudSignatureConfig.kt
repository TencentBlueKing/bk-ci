package com.tencent.devops.notify.tencentcloud.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Suppress("LongParameterList")
@Schema(title = "腾讯云签名配置信息")
open class TencentCloudSignatureConfig(
    @Schema(title = "接口请求方法")
    val httpRequestMethod: String,
    @Schema(title = "服务标识")
    val service: String,
    @Schema(title = "host地址")
    val host: String,
    @Schema(title = "请求地址")
    val url: String,
    @Schema(title = "操作的接口名称")
    val action: String,
    @Schema(title = "操作的 API 的版本")
    val version: String,
    @Schema(title = "签名方法，目前固定取该值TC3-HMAC-SHA256")
    val algorithm: String = "TC3-HMAC-SHA256",
    @Schema(title = "地域参数，用来标识希望操作哪个地域的数据")
    open val region: String,
    @Schema(title = "请求体")
    open val payload: String,
    @Schema(title = "密钥id")
    open val secretId: String,
    @Schema(title = "密钥key")
    open val secretKey: String
)
