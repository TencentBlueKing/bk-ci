package com.tencent.devops.openapi.service.op

interface OpAppUserService {
    fun checkUser(userId: String): Boolean
}
