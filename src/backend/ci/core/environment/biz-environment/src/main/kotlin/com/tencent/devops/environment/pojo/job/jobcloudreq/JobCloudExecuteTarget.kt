package com.tencent.devops.environment.pojo.job.jobcloudreq

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudExecuteTarget(
    @ApiModelProperty(value = "主机IP信息列表")
    @JsonProperty("ip_list")
    val ipList: List<JobCloudIpInfo>?,
    @ApiModelProperty(value = "主机ID列表")
    @JsonProperty("host_id_list")
    val hostIdList: List<Long>?
)