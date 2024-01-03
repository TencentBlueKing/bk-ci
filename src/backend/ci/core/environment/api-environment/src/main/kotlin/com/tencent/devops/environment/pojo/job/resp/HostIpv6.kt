package com.tencent.devops.environment.pojo.job.resp

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class HostIpv6(
    @ApiModelProperty(value = "主机ID")
    val bkHostId: Long,
    @ApiModelProperty(value = "管控区域ID")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP")
    val ip: String?,
    @ApiModelProperty(value = "Ipv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "Agent ID")
    val bkAgentId: String?,
    @ApiModelProperty(value = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)