package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "是否可以添加外部用户--请求")
data class OuterCanAddParam(
    @get:Schema(title = "用户列表,用英文,分隔", required = true)
    val userIds: String
)
