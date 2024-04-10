package com.tencent.devops.auth.resources

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.auth.api.ServiceSecurityResource
import com.tencent.devops.auth.service.TxSecurityService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource

@RestResource
class ServiceSecurityResourceImpl constructor(
    val securityService: TxSecurityService
) : ServiceSecurityResource {
    @AuditEntry(actionId = ActionId.PROJECT_USER_VERIFY)
    override fun verifyProjectUser(projectId: String, credentialKey: String): Result<Boolean?> {
        return securityService.verifyProjectUserByCredentialKey(
            projectId = projectId,
            credentialKey = credentialKey
        )
    }
    @AuditEntry(actionId = ActionId.WATER_MARK_GET)
    override fun getUserWaterMark(projectId: String, credentialKey: String): Result<String?> {
        return securityService.getUserWaterMarkByCredentialKey(
            credentialKey = credentialKey,
            projectId = projectId
        )
    }
}
