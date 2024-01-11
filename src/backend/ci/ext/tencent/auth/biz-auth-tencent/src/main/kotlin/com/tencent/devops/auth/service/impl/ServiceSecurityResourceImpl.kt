package com.tencent.devops.auth.service.impl

import com.tencent.devops.auth.api.ServiceSecurityResource
import com.tencent.devops.auth.service.TxSecurityService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceSecurityResourceImpl constructor(
    val securityService: TxSecurityService
) : ServiceSecurityResource {
    override fun verifyProjectUser(projectId: String, credentialKey: String): Result<Boolean?> {
        return securityService.verifyProjectUserByCredentialKey(
            projectId = projectId,
            credentialKey = credentialKey
        )
    }

    override fun getUserWaterMark(projectId: String, credentialKey: String): Result<String?> {
        return securityService.getUserWaterMarkByCredentialKey(
            credentialKey = credentialKey,
            projectId = projectId
        )
    }
}
