package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT_TOKEN
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.web.RequestFilter
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
@DependsOn("environmentUtil")
class ServiceSecurityFilter(
    private val jwtManager: JwtManager,
    private val servletRequest: HttpServletRequest
) : ContainerRequestFilter {

    companion object {
        private val excludeVeritfyPath = listOf("/api/swagger.json", "/api/external/service/versionInfo")
        private val logger = LoggerFactory.getLogger((ServiceSecurityFilter::class.java))
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        val uri = requestContext!!.uriInfo.requestUri.path
        if (shouldFilter(uri)) {

            val clientIp = servletRequest?.remoteAddr

            val jwt = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_JWT_TOKEN)
            if (jwt.isNullOrBlank()) {
                logger.warn("Invalid request, jwt is empty!Client ip:$clientIp,uri:$uri")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "Unauthorized:devops api jwt it empty."
                )
            }
            val checkResult: Boolean = jwtManager.verifyJwt(jwt)
            if (!checkResult) {
                logger.warn("Invalid request, jwt is invalid or expired!Client ip:$clientIp,uri:$uri")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = CommonMessageCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "Unauthorized:devops api jwt it invalid or expired."
                )
            }
        }
    }

    private fun shouldFilter(uri: String): Boolean {
        if (!jwtManager.isAuthEnable() || !EnvironmentUtil.isProdProfileActive()) {
            return false
        }
        // 不拦截的接口
        excludeVeritfyPath.forEach {
            if (uri.startsWith(it)) {
                return false
            }
        }
        // 拦截api接口
        if (uri.startsWith("/api/")) {
            return true
        }
        // 默认不拦截
        return false
    }
}