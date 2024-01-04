package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("返回值中的主机结构")
data class JobCloudHostInRes(
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    val ip: String?,
    @ApiModelProperty(value = "IPv6地址")
    val ipv6: String?,
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long?,
    @ApiModelProperty(value = "Agent ID")
    @JsonProperty("bk_agent_id")
    val bkAgentId: String?,
    @ApiModelProperty(value = "Agent是否正常，取值为：1-正常，0-异常")
    val alive: Int?
)