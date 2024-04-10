package com.tencent.devops.environment.pojo.job.jobcloudres

import io.swagger.v3.oas.annotations.media.Schema

data class JobCloudFileDestination(
    @get:Schema(title = "目标路径")
    val path: String,
    @get:Schema(title = "执行账号")
    val account: JobCloudAccount,
    @get:Schema(title = "分发目标机器")
    val server: JobCloudVariableServer
)