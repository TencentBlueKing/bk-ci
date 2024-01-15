package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.MemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "项目成员列表返回")
data class ProjectMembersVO(
    @Schema(name = "数量")
    val count: Int,
    @Schema(name = "成员信息列表")
    val results: Set<MemberInfo>
)
