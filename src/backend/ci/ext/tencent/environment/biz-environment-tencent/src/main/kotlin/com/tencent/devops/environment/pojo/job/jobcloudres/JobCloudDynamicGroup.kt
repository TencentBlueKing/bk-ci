package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudDynamicGroup(
    @get:Schema(title = "CMDB动态分组ID")
    val id: String
)