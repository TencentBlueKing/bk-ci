package com.tencent.devops.process.pojo.creative

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "创作流 Stage 审核卡片内容（CDS 预定义接口）")
data class CreativeStreamStageReviewContent(
    @get:Schema(title = "审批标题", required = true)
    val title: String,
    @get:Schema(title = "审批正文（Markdown）", required = true)
    val approvalContent: String,
    @get:Schema(title = "过期时间（ISO-8601）", required = false)
    val expireAt: String? = null
)
