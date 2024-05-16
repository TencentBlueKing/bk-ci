package com.tencent.devops.metrics.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目活跃用户数")
data class ProjectUserCountV0(
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "用户数")
    val userCount: Int,
    @get:Schema(title = "用户名单")
    val users: String
)
