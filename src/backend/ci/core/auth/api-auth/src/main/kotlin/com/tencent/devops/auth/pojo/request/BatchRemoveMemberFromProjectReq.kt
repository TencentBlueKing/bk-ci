package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量将用户移出项目")
data class BatchRemoveMemberFromProjectReq(
    @get:Schema(title = "目标对象")
    val targetMembers: List<ResourceMemberInfo>,
    @get:Schema(title = "授予人")
    val handoverTo: ResourceMemberInfo?
)
