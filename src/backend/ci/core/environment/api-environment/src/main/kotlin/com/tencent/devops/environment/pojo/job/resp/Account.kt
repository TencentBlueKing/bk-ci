package com.tencent.devops.environment.pojo.job.resp



data class Account(
    @get:Schema(title = "账号ID")
    val id: Long,
    @get:Schema(title = "账号名称")
    val name: String?,
    @get:Schema(title = "账号别名")
    val alias: String?
)