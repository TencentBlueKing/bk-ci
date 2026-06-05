package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "AI申请加入用户组请求体")
data class AiApplyJoinGroupReq(
    @get:Schema(title = "用户组ID列表", required = true)
    val groupIds: List<Int>,
    @get:Schema(title = "申请理由", required = true)
    val reason: String,
    @get:Schema(title = "期望时长(天)，默认180")
    val expiredDays: Int = 180
)
