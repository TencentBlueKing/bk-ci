package com.tencent.devops.openapi.filter.impl

import com.tencent.devops.auth.api.service.ServiceTokenResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.API_ACCESS_TOKEN_PROPERTY
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.filter.ApiFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
class SampleApiFilter constructor(
    private val client: Client
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(SampleApiFilter::class.java)
    }

    @Value("\${auth.accessToken.enabled:#{null}}")
    private val apiFilterEnabled: Boolean? = false

    override fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        val accessToken = requestContext.uriInfo.queryParameters.getFirst(API_ACCESS_TOKEN_PROPERTY)
        if (accessToken.isNullOrBlank()) {
            logger.warn("OPENAPI|verifyJWT accessToken is blank|" +
                "context=${requestContext.uriInfo.queryParameters}")
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request accessToken is empty.")
                    .build()
            )
            return false
        }
        try {
            val tokenInfo = client.get(ServiceTokenResource::class).validateToken(accessToken).data!!
            logger.info("OPENAPI|verifyJWT|accessToken=$accessToken|tokenInfo=$tokenInfo")
            requestContext.headers.add(AUTH_HEADER_USER_ID, tokenInfo.userId)
            return true
        } catch (ignore: Throwable) {
            logger.warn("OPENAPI|verifyJWT with error:", ignore)
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Verification failed : $ignore")
                    .build()
            )
        }
        return false
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (apiFilterEnabled != true) {
            return
        }
        if (!verifyJWT(requestContext)) {
            return
        }
    }
}
