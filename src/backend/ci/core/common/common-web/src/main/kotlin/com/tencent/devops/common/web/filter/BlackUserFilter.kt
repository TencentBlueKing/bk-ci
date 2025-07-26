package com.tencent.devops.common.web.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.web.RequestFilter
import jakarta.annotation.Priority
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

@Provider
@PreMatching
@Priority(1)
@RequestFilter
class BlackUserFilter : ContainerRequestFilter {

    @Value("\${black.users:admin}")
    private val blackUsers: List<String> = listOf("admin")

    override fun filter(requestContext: ContainerRequestContext) {
        val requestUri = requestContext.uriInfo.requestUri
        if (requestUri.path.contains("remotedev/get_all_windows_resource_quota")) {
            return
        }
        val userId = requestContext.getHeaderString(AUTH_HEADER_USER_ID)
        if (!userId.isNullOrBlank() && blackUsers.contains(userId.lowercase())) {
            logger.warn("User $userId is not allowed to access")
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_ILLEGAL_ACCESS_USER_ID,
                params = arrayOf(userId)
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BlackUserFilter::class.java)
    }
}
