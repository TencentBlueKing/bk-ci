package com.tencent.devops.project.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "按比例放量路由请求")
data class ProjectPercentageRoutingRequest(
    @get:JsonProperty(value = "targetPercent", required = true)
    @get:Schema(
        title = "目标百分比（1-100）",
        description = "targetPercent"
    )
    val targetPercent: Int,

    @get:JsonProperty(value = "sourceTag", required = true)
    @get:Schema(title = "源集群 consul tag", description = "sourceTag")
    val sourceTag: String,

    @get:JsonProperty(value = "targetTag", required = true)
    @get:Schema(title = "目标集群 consul tag", description = "targetTag")
    val targetTag: String,

    @get:JsonProperty(value = "channelCode", required = false)
    @get:Schema(
        title = "渠道代码，默认 BS",
        description = "channelCode"
    )
    val channelCode: String = "BS",

    @get:JsonProperty(value = "dryRun", required = false)
    @get:Schema(
        title = "是否预览模式（true=仅统计不落库，false=执行切换）",
        description = "dryRun"
    )
    val dryRun: Boolean = true
)
