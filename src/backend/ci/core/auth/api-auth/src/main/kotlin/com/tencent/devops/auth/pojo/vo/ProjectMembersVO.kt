package com.tencent.devops.auth.pojo.vo

import com.tencent.devops.auth.pojo.MemberInfo

data class ProjectMembersVO(
    val count: Int,
    val userIds: Set<MemberInfo>
)
