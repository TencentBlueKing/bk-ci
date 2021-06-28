package com.tencent.devops.openapi.service.op

class DefaultOpAppUserService : OpAppUserService {
    override fun checkUser(userId: String): Boolean {
        return true
    }
}
