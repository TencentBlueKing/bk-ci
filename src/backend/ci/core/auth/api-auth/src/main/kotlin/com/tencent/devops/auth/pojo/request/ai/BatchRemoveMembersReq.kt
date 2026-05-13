package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量移除用户组成员请求体")
data class BatchRemoveMembersReq(
    @get:Schema(title = "用户组ID列表", required = true)
    val groupIds: List<Int>,
    @get:Schema(title = "目标成员ID", required = true)
    val targetMemberId: String,
    @get:Schema(title = "权限交接人ID")
    val handoverToMemberId: String? = null
)
