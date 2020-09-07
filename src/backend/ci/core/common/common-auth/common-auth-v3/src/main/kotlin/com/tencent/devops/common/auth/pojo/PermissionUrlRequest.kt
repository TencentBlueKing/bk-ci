package com.tencent.devops.common.auth.pojo

data class PermissionUrlRequest(
    val system: String,
    val action: List<Action>
)