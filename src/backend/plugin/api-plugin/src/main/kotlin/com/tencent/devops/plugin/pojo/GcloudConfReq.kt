package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Gcloud请求报文")
data class GcloudConfReq(
    @ApiModelProperty("id")
    val id: Int,
    @ApiModelProperty("区域")
    val region: String,
    @ApiModelProperty("地址")
    val address: String,
    @ApiModelProperty("文件地址")
    val fileAddress: String,
    @ApiModelProperty("备注")
    val remark: String?
)
