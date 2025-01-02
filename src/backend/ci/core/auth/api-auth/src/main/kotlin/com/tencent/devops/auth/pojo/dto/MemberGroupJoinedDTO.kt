package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.enum.MemberType
import io.swagger.v3.oas.annotations.media.Schema

data class MemberGroupJoinedDTO(
    @get:Schema(title = "组id")
    val id: Int,
    @get:Schema(title = "组成员类型")
    val memberType: MemberType
)
