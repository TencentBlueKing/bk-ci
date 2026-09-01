package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "AI-批量将用户移出项目请求体")
data class AiBatchRemoveMemberFromProjectReq(
    @get:Schema(title = "目标成员ID列表")
    val targetMemberIds: List<String>,
    @get:Schema(title = "交接人用户ID（可选，检查结果不能直接移出时需传入）")
    val handoverToMemberId: String? = null
)
