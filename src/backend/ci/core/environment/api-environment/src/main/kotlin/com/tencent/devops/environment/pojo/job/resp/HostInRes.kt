package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回值中的主机结构")
data class HostInRes(
    @ApiModelProperty(value = "云区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "管控区域名称")
    val bkCloudName: String,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "IPv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long?,
    @ApiModelProperty(value = "Agent ID")
    val bkAgentId: String?,
    @ApiModelProperty(value = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)