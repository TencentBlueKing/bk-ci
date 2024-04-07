package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class JobCloudHost(
    @get:Schema(title = "主机ID")
    @JsonProperty("bk_host_id")
    var bkHostId: Long?,
    @get:Schema(title = "云区域ID")
    @JsonProperty("bk_cloud_id")
    var bkCloudId: Long?,
    @get:Schema(title = "IP地址")
    var ip: String? = ""
) {
    constructor(bkCloudId: Long, ip: String?) : this(null, bkCloudId, ip)
}