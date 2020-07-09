package com.tencent.devops.process.permission.service.impl

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode

class V3PipelinePermissionService constructor(
    authProjectApi: AuthProjectApi,
    authResourceApi: AuthResourceApi,
    authPermissionApi: AuthPermissionApi,
    pipelineAuthServiceCode: PipelineAuthServiceCode
) : AbstractPipelinePermissionService(
    authProjectApi = authProjectApi,
    authResourceApi = authResourceApi,
    authPermissionApi = authPermissionApi,
    pipelineAuthServiceCode = pipelineAuthServiceCode
) {

    override fun supplierForFakePermission(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }
}