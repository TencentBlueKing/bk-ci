package com.tencent.devops.common.auth.api.external

import com.tencent.devops.common.auth.api.pojo.external.response.AuthMgrResourceResponse

interface PermissionService {
    fun getMgrResource(projectId: String, resourceTypeCode: String, resourceCode: String): AuthMgrResourceResponse
}