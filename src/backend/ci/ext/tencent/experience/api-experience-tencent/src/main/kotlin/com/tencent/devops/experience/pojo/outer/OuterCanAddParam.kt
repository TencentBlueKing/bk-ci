package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "是否可以添加外部用户--请求")
data class OuterCanAddParam(
    @Schema(description = "用户列表,用英文,分隔", required = true)
    val userIds: String
)
