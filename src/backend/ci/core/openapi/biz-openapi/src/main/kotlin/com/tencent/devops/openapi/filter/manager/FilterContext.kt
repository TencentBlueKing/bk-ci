package com.tencent.devops.openapi.filter.manager

import com.tencent.devops.openapi.filter.manager.impl.ApiPathFilter.ApiType
import javax.ws.rs.container.ContainerRequestContext

data class FilterContext(
    val requestContext: ContainerRequestContext,
    var apiType: ApiType? = null,
    var needCheckPermissions: Boolean = false,
    var flowState: ApiFilterFlowState = ApiFilterFlowState.CONTINUE
)
