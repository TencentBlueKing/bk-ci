package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回值中的主机结构")
data class HostInRes(
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "IPv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?
)