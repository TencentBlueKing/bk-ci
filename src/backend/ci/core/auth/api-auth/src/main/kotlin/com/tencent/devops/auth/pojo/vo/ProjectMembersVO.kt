package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.MemberInfo
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "项目成员列表返回")
data class ProjectMembersVO(
    @get:Schema(title = "数量")
    val count: Int,
    @get:Schema(title = "成员信息列表")
    val results: Set<MemberInfo>
)
