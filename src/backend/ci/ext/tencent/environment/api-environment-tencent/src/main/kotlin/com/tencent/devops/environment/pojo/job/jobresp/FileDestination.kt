package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class FileDestination(
    @get:Schema(title = "目标路径")
    val path: String,
    @get:Schema(title = "执行账号")
    val account: Account,
    @get:Schema(title = "分发目标机器")
    val server: VariableServer
)