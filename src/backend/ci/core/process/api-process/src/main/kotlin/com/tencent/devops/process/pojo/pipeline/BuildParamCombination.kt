package com.tencent.devops.process.pojo.pipeline

import com.tencent.devops.common.web.annotation.BkField
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线启动参数组合")
data class BuildParamCombination(
    @get:Schema(description = "组合ID")
    val id: Long,
    @get:Schema(description = "组合名称")
    @field:BkField(minLength = 1, maxLength = 64)
    val name: String
)
