package com.tencent.devops.auth.provider.sample.service

import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.auth.service.AuthAuthorizationScopesService

class SampleAuthAuthorizationScopesService : AuthAuthorizationScopesService {
    override fun generateBkciAuthorizationScopes(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> = emptyList()

    override fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): List<AuthorizationScopes> = emptyList()
}
