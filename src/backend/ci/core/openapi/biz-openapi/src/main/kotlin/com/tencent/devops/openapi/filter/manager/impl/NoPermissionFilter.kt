package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_SECRET
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.openapi.utils.ApiGatewayUtil
import jakarta.ws.rs.container.ContainerRequestContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class NoPermissionFilter constructor(
    private val apiGatewayUtil: ApiGatewayUtil
) : ApiFilterManager {
    companion object {
        private val logger = LoggerFactory.getLogger(NoPermissionFilter::class.java)
        private const val appCodeHeader = "app_code"
        private const val appSecHeader = "app_secret"
    }

    @Value("\${api.blueKing.enable:#{null}}")
    private val apiBlueKingEnabled: Boolean? = false

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return apiBlueKingEnabled == true &&
            !apiGatewayUtil.isAuth()
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        // 将query中的app_code和app_secret设置成头部
        setupHeader(requestContext.requestContext)
        return ApiFilterFlowState.AUTHORIZED
    }

    private fun setupHeader(requestContext: ContainerRequestContext) {
        requestContext.uriInfo?.pathParameters?.forEach { pathParam ->
            if (pathParam.key == appCodeHeader && pathParam.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_CODE]?.set(0, pathParam.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, pathParam.value[0])
                }
            } else if (pathParam.key == appSecHeader && pathParam.value.isNotEmpty()) {
                requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, null)
                if (requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET] != null) {
                    requestContext.headers[AUTH_HEADER_DEVOPS_APP_SECRET]?.set(0, pathParam.value[0])
                } else {
                    requestContext.headers.add(AUTH_HEADER_DEVOPS_APP_CODE, pathParam.value[0])
                }
            }
        }
    }
}
