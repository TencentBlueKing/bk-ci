package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes

/**
 * 蓝盾权限对接监控平台配置类
 */
interface AuthAuthorizationScopesService {
    /**
     * 生成蓝盾平台授权范围
     */
    fun generateBkciAuthorizationScopes(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes>

    /**
     * 生成监控平台授权范围
     */
    fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String? = null
    ): List<AuthorizationScopes>
}
