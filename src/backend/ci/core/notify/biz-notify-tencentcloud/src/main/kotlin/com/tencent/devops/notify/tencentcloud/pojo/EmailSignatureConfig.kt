package com.tencent.devops.notify.tencentcloud.pojo

import io.swagger.annotations.ApiModelProperty

data class EmailSignatureConfig(
    @ApiModelProperty("请求体")
    override val payload: String,
    @ApiModelProperty("密钥id")
    override val secretId: String,
    @ApiModelProperty("密钥key")
    override val secretKey: String,
    @ApiModelProperty("地域参数，用来标识希望操作哪个地域的数据")
    override val region: String
) : TencentCloudSignatureConfig(
    httpRequestMethod = "POST",
    service = "ses",
    host = "ses.tencentcloudapi.com",
    url = "https://ses.tencentcloudapi.com",
    action = "SendEmail",
    version = "2020-10-02",
    payload = payload,
    secretId = secretId,
    secretKey = secretKey,
    region = region
)
