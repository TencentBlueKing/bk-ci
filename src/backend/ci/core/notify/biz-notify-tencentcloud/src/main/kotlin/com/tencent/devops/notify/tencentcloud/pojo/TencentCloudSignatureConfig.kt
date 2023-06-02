package com.tencent.devops.notify.tencentcloud.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("腾讯云签名配置信息")
open class TencentCloudSignatureConfig(
    @ApiModelProperty("接口请求方法")
    val httpRequestMethod: String,
    @ApiModelProperty("服务标识")
    val service: String,
    @ApiModelProperty("host地址")
    val host: String,
    @ApiModelProperty("请求地址")
    val url: String,
    @ApiModelProperty("操作的接口名称")
    val action: String,
    @ApiModelProperty("操作的 API 的版本")
    val version: String,
    @ApiModelProperty("签名方法，目前固定取该值TC3-HMAC-SHA256")
    val algorithm: String = "TC3-HMAC-SHA256",
    @ApiModelProperty("地域参数，用来标识希望操作哪个地域的数据")
    open val region: String,
    @ApiModelProperty("请求体")
    open val payload: String,
    @ApiModelProperty("密钥id")
    open val secretId: String,
    @ApiModelProperty("密钥key")
    open val secretKey: String
)
