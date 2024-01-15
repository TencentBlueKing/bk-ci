package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "外部用户--选择信息")
data class OuterSelectorVO(
    @Schema(description = "ID")
    val username: String
)
