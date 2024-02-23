package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudAccount(
    @get:Schema(title = "账号ID")
    val id: Long,
    @get:Schema(title = "账号名称")
    val name: String?,
    @get:Schema(title = "账号别名")
    val alias: String?
)