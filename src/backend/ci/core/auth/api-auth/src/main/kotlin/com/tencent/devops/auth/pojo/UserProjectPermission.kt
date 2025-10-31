package com.tencent.devops.auth.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "成员项目权限")
data class UserProjectPermission(
    val memberId: String,
    val projectCode: String,
    val action: String,
    val iamGroupId: Int,
    val expireTime: LocalDateTime
)
