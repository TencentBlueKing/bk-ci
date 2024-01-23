package com.tencent.devops.environment.pojo.job.jobcloudres

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JobCloudDynamicGroup(
    @ApiModelProperty(value = "CMDB动态分组ID")
    val id: String
)