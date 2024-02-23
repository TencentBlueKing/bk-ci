package com.tencent.devops.project.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "高级用户实体")
data class SeniorUserDTO(
    @get:Schema(title = "用户ID")
    val userId: String,
    @get:Schema(title = "用户名称")
    val name: String,
    @get:Schema(title = "bg名称")
    val bgName: String
)
