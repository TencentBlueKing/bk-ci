package com.tencent.devops.auth.service.oauth2.grant

interface TokenGranter {
    fun grant(grantType: String): String?
}
