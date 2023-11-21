package com.tencent.devops.environment.pojo.job.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class HostIpv6(
    @ApiModelProperty(value = "主机ID")
    val hostList: Long,
    @ApiModelProperty(value = "IP")
    val ip: String,
    @ApiModelProperty(value = "Ipv6地址")
    val ipv6: String
)