package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "移出项目成员请求体")
data class AiRemoveMemberFromProjectReq(
    @get:Schema(title = "要移除的成员ID", required = true)
    val targetMemberId: String,
    @get:Schema(title = "权限交接人ID")
    val handoverToMemberId: String? = null
)
