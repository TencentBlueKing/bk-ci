package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

data class JobCloudHostIpv6(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    val bkHostId: Long,
    @ApiModelProperty(value = "管控区域ID")
    @JsonProperty("bk_cloud_id")
    val bkCloudId: Long?,
    @ApiModelProperty(value = "IP")
    val ip: String?,
    @ApiModelProperty(value = "Ipv6地址")
    @JsonProperty("host_list")
    val ipv6: String?
)