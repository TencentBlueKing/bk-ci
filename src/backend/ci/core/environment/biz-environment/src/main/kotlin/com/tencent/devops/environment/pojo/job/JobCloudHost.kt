package com.tencent.devops.environment.pojo.job

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudHost(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    var bkHostId: Long? = 0L,
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    var bkCloudId: Long? = 0L,
    @ApiModelProperty(value = "IP地址")
    @JsonProperty("ip")
    var ip: String? = ""
)