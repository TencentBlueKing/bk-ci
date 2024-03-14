package com.tencent.devops.environment.pojo.job.jobreq

import io.swagger.v3.oas.annotations.media.Schema

data class CreateAccountReq(
    @get:Schema(title = "帐号名称", required = true)
    val account: String,
    @get:Schema(title = "账号类型", description = "1：Linux，2：Windows", required = true)
    val type: Int,
    @get:Schema(title = "账号用途", description = "1：系统账号", required = true)
    val category: Int,
    @get:Schema(title = "系统账号密码", description = "账号用途为系统账号 且 账号类型为Windows时，必传。")
    val password: String?,
    @get:Schema(title = "账号别名", description = "不传则以账号名称作为别名。")
    val alias: String?,
    @get:Schema(title = "账号描述")
    val description: String?
)