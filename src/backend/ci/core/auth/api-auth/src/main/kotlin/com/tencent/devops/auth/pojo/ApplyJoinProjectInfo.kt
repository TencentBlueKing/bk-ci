package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目-申请加入项目实体类")
data class ApplyJoinProjectInfo(
    @get:Schema(title = "过期时间")
    val expireTime: String,
    @get:Schema(title = "申请理由")
    val reason: String
)
