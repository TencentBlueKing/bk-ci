package com.tencent.bkrepo.auth.pojo.role

data class UpdateRoleRequest(
    val name: String?,
    val description: String?,
    val userIds: Set<String>?
)
