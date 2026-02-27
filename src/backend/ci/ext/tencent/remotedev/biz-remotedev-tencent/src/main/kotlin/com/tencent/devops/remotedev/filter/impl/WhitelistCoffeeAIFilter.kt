package com.tencent.devops.remotedev.filter.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.filter.ApiFilter
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
@PreMatching
@RequestFilter
class WhitelistCoffeeAIFilter constructor(
    private val cacheService: ConfigCacheService
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(WhitelistCoffeeAIFilter::class.java)
    }


    override fun verify(requestContext: ContainerRequestContext): Boolean {
        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path

        if (!path.contains("/api/user/ai/")) return true

        val userId = requestContext.headers[AUTH_HEADER_DEVOPS_USER_ID]?.get(0) ?: run {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Sorry, X-DEVOPS-UID is null.")
                    .build()
            )
            return true
        }
        if (!cacheService.checkApiCoffeeAIWhiteList(userId)) {
            logger.info("user($userId)wants to access the resource($path), but is blocked.")
            return false
        }
        return true
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verify(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = ErrorCodeEnum.DENIAL_OF_SERVICE.errorCode,
                            params = null,
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                        )
                    )
                    .build()
            )
            return
        }
    }
}
