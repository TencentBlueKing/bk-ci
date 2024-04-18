package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "源文件帐号信息")
data class Account(
    @get:Schema(title = "源执行账号别名", description = "可从账号页面获取，与id必须存在一个，同时存在时，id优先。")
    val alias: String?,
    @get:Schema(title = "源执行帐号ID", description = "与alias必须存在一个，同时存在时，id优先。")
    val id: Long?
)