package com.tencent.devops.environment.pojo.job.resp



data class FileDestination(
    @get:Schema(title = "目标路径")
    val path: String,
    @get:Schema(title = "执行账号")
    val account: Account,
    @get:Schema(title = "分发目标机器")
    val server: VariableServer
)