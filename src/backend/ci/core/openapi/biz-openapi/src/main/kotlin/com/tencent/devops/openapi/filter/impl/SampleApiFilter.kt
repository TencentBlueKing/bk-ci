package com.tencent.devops.openapi.filter.impl

import com.tencent.devops.auth.api.service.ServiceTokenResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.openapi.filter.ApiFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${auth.accessToken.queryParamName:#{null}}")
    private val headerName: String? = null

    override fun verifyJWT(requestContext: ContainerRequestContext): Boolean {
        val bkApiJwt = requestContext.uriInfo.queryParameters.getFirst(headerName)
        if (bkApiJwt.isNullOrBlank()) {
            logger.error("Request bk api jwt is empty for ${requestContext.request}")
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request bkapi jwt is empty.")
                    .build()
            )
            return false
        }
        return client.get(ServiceTokenResource::class).validateToken(token = bkApiJwt).data ?: false
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verifyJWT(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Devops OpenAPI Auth fail：user or app auth fail.")
                    .build()
            )
            return
        }
    }
}