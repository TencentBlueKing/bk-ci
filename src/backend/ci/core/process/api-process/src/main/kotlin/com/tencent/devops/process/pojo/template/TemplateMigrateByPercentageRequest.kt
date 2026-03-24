package com.tencent.devops.process.pojo.template

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按比例灰度迁移模板请求")
data class TemplateMigrateByPercentageRequest(
    @get:JsonProperty(value = "targetPercent", required = true)
    @get:Schema(title = "目标百分比（1-100）", description = "targetPercent")
    val targetPercent: Int,

    @get:JsonProperty(value = "channelCode", required = false)
    @get:Schema(title = "渠道代码，默认 BS", description = "channelCode")
    val channelCode: String = "BS",

    @get:JsonProperty(value = "dryRun", required = false)
    @get:Schema(
        title = "是否预览模式（true=仅统计不触发迁移，false=触发异步迁移）",
        description = "dryRun"
    )
    val dryRun: Boolean = true
)
