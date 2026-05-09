package com.tencent.devops.process.pojo.template.v2

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "修复模板错误参数-模板问题版本")
data class FixBadParamsItem(
    @get:Schema(title = "模板ID")
    val templateId: String,
    @get:Schema(title = "有问题的版本号列表")
    val versions: List<Long>
)
