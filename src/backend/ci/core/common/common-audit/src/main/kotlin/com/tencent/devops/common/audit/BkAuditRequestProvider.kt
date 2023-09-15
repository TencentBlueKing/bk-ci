package com.tencent.devops.common.audit

import com.tencent.bk.audit.AuditRequestProvider
import com.tencent.bk.audit.constants.AccessTypeEnum
import com.tencent.bk.audit.constants.UserIdentifyTypeEnum
import com.tencent.bk.audit.exception.AuditException
import com.tencent.bk.audit.model.AuditHttpRequest
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

class BkAuditRequestProvider : AuditRequestProvider {
    companion object {
        private const val HEADER_USERNAME = AUTH_HEADER_USER_ID
        private const val HEADER_USER_IDENTIFY_TENANT_ID = "X-User-Identify-Tenant-Id"
        private const val HEADER_USER_IDENTIFY_TYPE = "X-User-Identify-Type"
        private const val HEADER_ACCESS_TYPE = "USER-AGENT"
        private const val HEADER_REQUEST_ID = "X-DEVOPS-RID"
        private const val HEADER_BK_APP_CODE = AUTH_HEADER_DEVOPS_APP_CODE
        private val logger = LoggerFactory.getLogger(BkAuditRequestProvider::class.java)
    }

    override fun getRequest(): AuditHttpRequest {
        val httpServletRequest: HttpServletRequest = getHttpServletRequest()
        return AuditHttpRequest(httpServletRequest)
    }

    private fun getHttpServletRequest(): HttpServletRequest {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes == null) {
            logger.error("Could not get RequestAttributes from RequestContext!")
            throw AuditException("Parse http request error")
        }
        return (requestAttributes as ServletRequestAttributes).request
    }

    override fun getUsername(): String? {
        val httpServletRequest = getHttpServletRequest()
        return httpServletRequest.getHeader(HEADER_USERNAME)
    }

    override fun getUserIdentifyType(): UserIdentifyTypeEnum? {
        val httpServletRequest = getHttpServletRequest()
        return UserIdentifyTypeEnum.valOf(
            httpServletRequest.getHeader(HEADER_USER_IDENTIFY_TYPE)
        )
    }

    override fun getUserIdentifyTenantId(): String? {
        val httpServletRequest = getHttpServletRequest()
        return httpServletRequest.getHeader(HEADER_USER_IDENTIFY_TENANT_ID)
    }

    override fun getAccessType(): AccessTypeEnum {
        val httpServletRequest = getHttpServletRequest()
        val accessTypeHeader = httpServletRequest.getHeader(HEADER_ACCESS_TYPE)
        return when {
            accessTypeHeader.contains("Mozilla") -> AccessTypeEnum.WEB
            accessTypeHeader.contains("API") -> AccessTypeEnum.API
            else -> AccessTypeEnum.OTHER
        }
    }

    override fun getRequestId(): String? {
        val httpServletRequest = getHttpServletRequest()
        return httpServletRequest.getHeader(HEADER_REQUEST_ID)
    }

    override fun getBkAppCode(): String? {
        val httpServletRequest = getHttpServletRequest()
        return httpServletRequest.getHeader(HEADER_BK_APP_CODE)
    }

    override fun getClientIp(): String? {
        val request = getHttpServletRequest()
        val xff = request.getHeader("X-Forwarded-For")
        return if (xff == null) {
            request.remoteAddr
        } else {
            if (xff.contains(",")) xff.split(",".toRegex()).toTypedArray()[0] else xff
        }
    }

    override fun getUserAgent(): String? {
        val request = getHttpServletRequest()
        return request.getHeader("User-Agent")
    }
}
