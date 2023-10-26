package com.tencent.devops.environment.pojo.job.req

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudHost(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    var bkHostId: Long?,
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    var bkCloudId: Long?,
    @ApiModelProperty(value = "IP地址")
    @JsonProperty("ip")
    var ip: String? = ""
)