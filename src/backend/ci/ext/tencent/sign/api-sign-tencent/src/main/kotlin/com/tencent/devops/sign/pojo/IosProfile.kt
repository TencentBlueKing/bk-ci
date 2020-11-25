package com.tencent.devops.sign.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("企业内测环境的证书和描述文件列表")
data class IosProfile(
    @ApiModelProperty("证书ID", required = true)
    val id: String,
    @ApiModelProperty("证书对应的Bundle ID", required = true)
    val bundleId: String,
    @ApiModelProperty("证书类型", required = true)
    val category: String,
    @ApiModelProperty("证书名称", required = true)
    val cerName: String,
    @ApiModelProperty("证书环境", required = true)
    val environment: String,
    @ApiModelProperty("证书文件名", required = true)
    val filename: String
)