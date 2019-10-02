package com.tencent.devops.plugin.pojo.zhiyun

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("织云业务信息")
data class ZhiyunProduct(
    @ApiModelProperty("业务id")
    val productId: String,
    @ApiModelProperty("业务名称")
    val productName: String
)