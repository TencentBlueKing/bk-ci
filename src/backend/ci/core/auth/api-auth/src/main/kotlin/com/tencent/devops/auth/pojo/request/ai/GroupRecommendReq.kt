package com.tencent.devops.auth.pojo.request.ai

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "用户组推荐请求体")
data class GroupRecommendReq(
    @get:Schema(title = "资源类型", required = true)
    val resourceType: String,
    @get:Schema(title = "资源Code", required = true)
    val resourceCode: String,
    @get:Schema(title = "目标操作权限", required = true)
    val action: String,
    @get:Schema(title = "目标用户ID", required = true)
    val targetUserId: String
)
