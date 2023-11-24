package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class HostIpv6(
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long,
    @ApiModelProperty(value = "管控区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP")
    val ip: String?,
    @ApiModelProperty(value = "Ipv6地址")
    val ipv6: String?
)