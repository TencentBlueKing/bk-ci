package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "外部用户--选择信息")
data class OuterSelectorVO(
    @get:Schema(title = "ID")
    val username: String
)
