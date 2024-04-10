package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class DynamicGroup(
    @get:Schema(title = "CMDB动态分组ID")
    val id: String
)