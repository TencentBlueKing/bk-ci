package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.auth.api.service.ServiceTokenResource
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.API_ACCESS_TOKEN_PROPERTY
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import javax.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SampleApiFilter constructor(
    private val client: Client
) : ApiFilterManager {
    companion object {
        private val logger = LoggerFactory.getLogger(SampleApiFilter::class.java)
    }

    @Value("\${auth.accessToken.enabled:#{null}}")
    private val apiFilterEnabled: Boolean? = false

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return apiFilterEnabled == true
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val accessToken = requestContext.requestContext.uriInfo.queryParameters.getFirst(API_ACCESS_TOKEN_PROPERTY)
        if (accessToken.isNullOrBlank()) {
            logger.warn(
                "OPENAPI|verifyJWT accessToken is blank|" +
                    "context=${requestContext.requestContext.uriInfo.queryParameters}"
            )
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Request accessToken is empty.")
                    .build()
            )
            return ApiFilterFlowState.BREAK
        }
        try {
            val tokenInfo = client.get(ServiceTokenResource::class).validateToken(accessToken).data!!
            logger.info("OPENAPI|verifyJWT|accessToken=$accessToken|tokenInfo=$tokenInfo")
            requestContext.requestContext.headers.add(AUTH_HEADER_USER_ID, tokenInfo.userId)
            return ApiFilterFlowState.CONTINUE
        } catch (ignore: Throwable) {
            logger.warn("OPENAPI|verifyJWT with error:", ignore)
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("Verification failed : $ignore")
                    .build()
            )
            return ApiFilterFlowState.BREAK
        }
    }
}
