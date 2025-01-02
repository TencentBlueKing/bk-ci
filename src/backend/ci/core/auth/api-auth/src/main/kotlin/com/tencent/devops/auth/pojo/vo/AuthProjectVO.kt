package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目返回体")
data class AuthProjectVO(
    @get:Schema(title = "数量")
    val projectCode: String,
    @get:Schema(title = "项目")
    val projectName: String
)
