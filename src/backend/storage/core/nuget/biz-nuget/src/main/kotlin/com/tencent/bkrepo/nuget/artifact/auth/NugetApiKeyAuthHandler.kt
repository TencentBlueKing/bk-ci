package com.tencent.bkrepo.nuget.artifact.auth

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.security.http.core.HttpAuthHandler
import com.tencent.bkrepo.common.security.http.credentials.AnonymousCredentials
import com.tencent.bkrepo.common.security.http.credentials.HttpAuthCredentials
import com.tencent.bkrepo.common.security.http.credentials.UsernamePasswordCredentials
import com.tencent.bkrepo.common.security.manager.AuthenticationManager
import com.tencent.bkrepo.common.service.util.HeaderUtils
import org.apache.commons.lang.StringUtils
import javax.servlet.http.HttpServletRequest

/**
 * 限定nuget set api key使用用户名+密码模式来进行认证
 */
class NugetApiKeyAuthHandler(
    private val authenticationManager: AuthenticationManager
) : HttpAuthHandler {
    override fun extractAuthCredentials(request: HttpServletRequest): HttpAuthCredentials {
        val apiKey = HeaderUtils.getHeader("X-NuGet-ApiKey").orEmpty()
        return if (StringUtils.isNotEmpty(apiKey)) {
            val parts = apiKey.trim().split(StringPool.COLON)
            require(parts.size >= 2)
            UsernamePasswordCredentials(parts[0], parts[1])
        } else AnonymousCredentials()
    }

    override fun onAuthenticate(request: HttpServletRequest, authCredentials: HttpAuthCredentials): String {
        require(authCredentials is UsernamePasswordCredentials)
        return authenticationManager.checkUserAccount(authCredentials.username, authCredentials.password)
    }
}
