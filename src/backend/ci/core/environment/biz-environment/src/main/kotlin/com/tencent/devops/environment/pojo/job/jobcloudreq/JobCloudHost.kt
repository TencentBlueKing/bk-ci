package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudHost(
    @ApiModelProperty(value = "主机ID")
    @JsonProperty("bk_host_id")
    var bkHostId: Long?,
    @ApiModelProperty(value = "云区域ID")
    @JsonProperty("bk_cloud_id")
    var bkCloudId: Long? = 0,
    @ApiModelProperty(value = "IP地址")
    var ip: String? = ""
) {
    constructor(bkCloudId: Long, ip: String?) : this(null, bkCloudId, ip)
}