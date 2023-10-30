package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("主机结构")
data class Host(
    @ApiModelProperty(value = "云区域ID", required = true)
    val bkCloudId: Long = 0,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?
)