package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_JWT
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.constant.SecurityErrorCode
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class ServiceSecurityFilter(
    private val jwtManager: JwtManager
) : ContainerRequestFilter {

    companion object {
        private val logger = LoggerFactory.getLogger((ServiceSecurityFilter::class.java))
    }

    override fun filter(requestContext: ContainerRequestContext?) {
        if (shouldFilter(requestContext!!)) {
            val jwt = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_JWT)
            if (jwt.isNullOrBlank()) {
                logger.warn("Invalid request, jwt is empty!")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = SecurityErrorCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "禁止访问微服务"
                )
            }
            val checkResult: Boolean = jwtManager.verifyJwt(jwt)
            if (!checkResult) {
                logger.warn("Invalid request, jwt is invalid or expired!")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = SecurityErrorCode.ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "禁止访问微服务"
                )
            }
        }
    }

    private fun shouldFilter(requestContext: ContainerRequestContext): Boolean {
        val uri = requestContext.uriInfo.requestUri.path
        logger.info("HttpServletRequest.requestURI=$uri")
        if (!jwtManager.isAuth()) {
            return false
        }
//        // dev环境需要支持swagger，请求无需认证
//        if (!EnvironmentUtil.isProdProfileActive()) {
//            return false
//        }
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/api/")
    }
}