package com.tencent.bk.codecc.task.service.impl

import com.tencent.devops.common.auth.api.external.PermissionService
import com.tencent.devops.common.auth.api.pojo.external.response.AuthMgrResourceResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TencentPermissionService @Autowired constructor(
): PermissionService {
    override fun getMgrResource(projectId: String, resourceTypeCode: String, resourceCode: String): AuthMgrResourceResponse {
        TODO()
    }
}