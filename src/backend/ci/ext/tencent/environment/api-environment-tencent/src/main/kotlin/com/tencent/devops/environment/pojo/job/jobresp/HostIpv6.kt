package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class HostIpv6(
    @get:Schema(title = "主机ID")
    val bkHostId: Long,
    @get:Schema(title = "管控区域ID")
    val bkCloudId: Long?,
    @get:Schema(title = "管控区域名称")
    val bkCloudName: String?,
    @get:Schema(title = "IP")
    val ip: String?,
    @get:Schema(title = "Ipv6地址")
    val ipv6: String?,
    @get:Schema(title = "Agent ID")
    val bkAgentId: String?,
    @get:Schema(title = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)