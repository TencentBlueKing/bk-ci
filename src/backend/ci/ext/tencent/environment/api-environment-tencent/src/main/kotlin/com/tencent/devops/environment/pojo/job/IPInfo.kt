package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IP结构")
data class IPInfo(
    @ApiModelProperty(value = "云区域ID", required = true)
    val bkCloudId: Long,
    @ApiModelProperty(value = "IP地址", required = true)
    val ip: String
)