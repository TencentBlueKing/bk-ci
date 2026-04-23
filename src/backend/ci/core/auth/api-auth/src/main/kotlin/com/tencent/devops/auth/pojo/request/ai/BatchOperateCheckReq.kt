package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量操作用户组成员检查请求体")
data class BatchOperateCheckReq(
    @get:Schema(title = "用户组ID列表", required = true)
    val groupIds: List<Int>,
    @get:Schema(title = "目标成员ID", required = true)
    val targetMemberId: String
)
