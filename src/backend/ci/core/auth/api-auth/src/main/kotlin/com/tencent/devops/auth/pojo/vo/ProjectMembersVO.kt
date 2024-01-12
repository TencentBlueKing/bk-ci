package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.MemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "项目成员列表返回")
data class ProjectMembersVO(
    @Schema(description = "数量")
    val count: Int,
    @Schema(description = "成员信息列表")
    val results: Set<MemberInfo>
)
