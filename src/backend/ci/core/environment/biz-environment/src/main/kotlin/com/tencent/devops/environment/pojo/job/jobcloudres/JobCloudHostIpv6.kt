package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudHostIpv6(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long,
    @ApiModelProperty(value = "管控区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "管控区域名称")
    @JsonProperty("bk_cloud_name")
    val bkCloudName: String?,
    @ApiModelProperty(value = "IP")
    val ip: String?,
    @ApiModelProperty(value = "Ipv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "Agent ID")
    @JsonProperty("bk_agent_id")
    val bkAgentId: String?,
    @ApiModelProperty(value = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)