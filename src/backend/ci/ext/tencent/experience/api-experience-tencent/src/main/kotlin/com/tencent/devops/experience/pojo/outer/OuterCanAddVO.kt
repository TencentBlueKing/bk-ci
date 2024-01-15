package com.tencent.devops.experience.pojo.outer

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "是否可以添加外部用户--返回")
data class OuterCanAddVO(
    @Schema(description = "合法人员列表")
    val legalUserIds: List<String>,
    @Schema(description = "不合法人员列表")
    val illegalUserIds: List<String>
)
