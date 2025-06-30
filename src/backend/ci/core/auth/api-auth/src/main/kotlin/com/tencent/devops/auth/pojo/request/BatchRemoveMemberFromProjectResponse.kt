package com.tencent.devops.auth.pojo.request

import com.tencent.devops.auth.pojo.ResourceMemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量将用户移出项目")
data class BatchRemoveMemberFromProjectResponse(
    @get:Schema(title = "用户")
    val users: List<ResourceMemberInfo>,
    @get:Schema(title = "部门")
    val departments: List<ResourceMemberInfo>
)
