package com.tencent.devops.environment.pojo.job.req

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@Suppress("ALL")
data class JobCloudExecuteTarget(
    @ApiModelProperty(value = "主机列表")
    @JsonProperty("ip_list")
    val hostList: List<JobCloudHost>?
)