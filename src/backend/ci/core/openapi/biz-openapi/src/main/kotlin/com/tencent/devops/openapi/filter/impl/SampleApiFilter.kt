package com.tencent.devops.openapi.filter.impl

import com.tencent.devops.auth.api.service.ServiceTokenResource
import com.tencent.devops.common.api.constant.API_ACCESS_TOKEN_PROPERTY
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.filter.ApiFilter
import org.jooq.True
import org.slf4j.LoggerFactory
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.PreMatching
import javax.ws.rs.core.Response
import javax.ws.rs.ext.Provider

@Provider
@PreMatching
@RequestFilter
@Suppress("UNUSED")
class SampleApiFilter constructor(
    private val client: Client
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(SampleApiFilter::class.java)
    }

    override fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        val accessToken = requestContext.uriInfo.queryParameters.getFirst(API_ACCESS_TOKEN_PROPERTY)
        if (accessToken.isNullOrBlank()) {
            logger.error("Request accessToken is empty for ${requestContext.request}")
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request accessToken is empty.")
                    .build()
            )
            return false
        }
        try {
            client.get(ServiceTokenResource::class).validateToken(accessToken)
            return true
        } catch (ignore: Throwable) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Verification failed : $ignore")
                    .build()
            )
        }
        return false
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verifyJWT(requestContext)) {
            return
        }
    }
}