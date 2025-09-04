package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Suppress("ALL")
data class JobCloudExecuteTarget(
    @get:Schema(title = "主机IP信息列表")
    @JsonProperty("ip_list")
    val ipList: List<JobCloudIpInfo>?,
    @get:Schema(title = "主机ID列表")
    @JsonProperty("host_id_list")
    val hostIdList: List<Long>?
)