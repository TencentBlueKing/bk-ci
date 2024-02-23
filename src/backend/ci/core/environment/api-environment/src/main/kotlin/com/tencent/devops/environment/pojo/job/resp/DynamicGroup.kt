package com.tencent.devops.environment.pojo.job.resp



data class DynamicGroup(
    @get:Schema(title = "CMDB动态分组ID")
    val id: String
)