package com.tencent.devops.auth.service

interface UserProjectPermissionService {

    fun checkMember(
        projectCode: String,
        userId: String
    ): Boolean
}