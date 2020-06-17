package com.tencent.devops.common.web.interceptor

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.security.autoconfig.ServiceSecurityProperties
import com.tencent.devops.common.security.jwt.JwtManager
import com.tencent.devops.common.security.util.EnvironmentUtil
import com.tencent.devops.common.web.RequestInterceptor
import com.tencent.devops.common.web.constant.SecurityErrorCode.ERROR_SERVICE_NO_AUTH
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 服务认证拦截器
 */
@RequestInterceptor
class ServiceSecurityInterceptor @Autowired constructor(
    private val jwtManager: JwtManager,
    private val securityProperties: ServiceSecurityProperties
) : HandlerInterceptorAdapter() {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        if (shouldFilter(request)) {
            val jwt = request.getHeader("x-job-auth-token")
            if (StringUtils.isEmpty(jwt)) {
                logger.warn("Invalid request, jwt is empty!")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "无访访问服务权限"
                )
            }
            val checkResult: Boolean = jwtManager.verifyJwt(jwt)
            if (!checkResult) {
                logger.warn("Invalid request, jwt is invalid or expired!")
                throw ErrorCodeException(
                    statusCode = 401,
                    errorCode = ERROR_SERVICE_NO_AUTH,
                    defaultMessage = "无访访问服务权限"
                )
            }
        }
        return true
    }

    override fun postHandle(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        handler: Any?,
        modelAndView: ModelAndView?
    ) {
        TODO("Not yet implemented")
    }

    private fun shouldFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        logger.info("HttpServletRequest.requestURI=$uri")
        if (securityProperties.enable == null || !securityProperties.enable!!) {
            return false
        }
        // dev环境需要支持swagger，请求无需认证
        if (!EnvironmentUtil.isProdProfileActive()) {
            return false
        }
        // 只拦截web/service/esb的API请求
        return uri.startsWith("/web/") || uri.startsWith("/service/") || uri.startsWith("/esb/")
    }

    companion object {
        private val logger = LoggerFactory.getLogger((ServiceSecurityInterceptor::class.java))
    }
}
