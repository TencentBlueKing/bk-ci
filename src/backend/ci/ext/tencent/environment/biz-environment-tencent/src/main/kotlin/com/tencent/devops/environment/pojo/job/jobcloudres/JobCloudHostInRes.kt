package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "返回值中的主机结构")
data class JobCloudHostInRes(
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @get:Schema(title = "管控区域名称")
    @JsonProperty("bk_cloud_name")
    val bkCloudName: String?,
    @get:Schema(title = "IP地址")
    val ip: String?,
    @get:Schema(title = "IPv6地址")
    val ipv6: String?,
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @get:Schema(title = "Agent ID")
    @JsonProperty("bk_agent_id")
    val bkAgentId: String?,
    @get:Schema(title = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)