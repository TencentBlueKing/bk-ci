package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class DynamicGroup(
    @ApiModelProperty(value = "CMDB动态分组ID")
    val id: String
)