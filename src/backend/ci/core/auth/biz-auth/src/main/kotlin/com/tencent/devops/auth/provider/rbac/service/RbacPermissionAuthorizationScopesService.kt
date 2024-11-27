package com.tencent.devops.auth.provider.rbac.service

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.auth.service.AuthAuthorizationScopesService
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.common.api.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class RbacPermissionAuthorizationScopesService constructor(
    private val authMonitorSpaceService: AuthMonitorSpaceService,
    private val iamConfiguration: IamConfiguration
) : AuthAuthorizationScopesService {
    @Value("\${monitor.register:false}")
    private val registerMonitor: Boolean = false

    @Value("\${monitor.iamSystem:}")
    private val monitorSystemId = ""

    override fun generateBkciAuthorizationScopes(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> {
        return buildAuthorizationScopes(
            systemId = iamConfiguration.systemId,
            authorizationScopesStr = authorizationScopesStr,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
    }

    override fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): List<AuthorizationScopes> {
        if (!registerMonitor)
            return listOf()
        val spaceBizId = authMonitorSpaceService.getOrCreateMonitorSpace(
            projectName = projectName,
            projectCode = projectCode,
            groupCode = groupCode,
            userId = userId
        )
        logger.info("RbacPermissionMonitorService|generateMonitorAuthorizationScopes|$spaceBizId")
        return buildAuthorizationScopes(
            systemId = monitorSystemId,
            authorizationScopesStr = authMonitorSpaceService.getMonitorGroupConfig(groupCode)!!,
            projectCode = "-$spaceBizId",
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
    }

    @Suppress("LongParameterList")
    private fun buildAuthorizationScopes(
        systemId: String,
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> {
        val replaceAuthorizationScopesStr =
            authorizationScopesStr.replace(SYSTEM_PLACEHOLDER, systemId)
                .replace(PROJECT_ID_PLACEHOLDER, projectCode)
                .replace(PROJECT_NAME_PLACEHOLDER, projectName)
                .replace(RESOURCE_CODE_PLACEHOLDER, iamResourceCode)
                // 如果资源名中有\,需要转义,不然json序列化时会报错
                .replace(RESOURCE_NAME_PLACEHOLDER, resourceName.replace("\\", "\\\\"))
        logger.info("$systemId|$projectCode authorization scopes after replace $replaceAuthorizationScopesStr ")
        return JsonUtil.to(replaceAuthorizationScopesStr, object : TypeReference<List<AuthorizationScopes>>() {})
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionAuthorizationScopesService::class.java)
        private const val SYSTEM_PLACEHOLDER = "#system#"
        private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
        private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
        private const val RESOURCE_CODE_PLACEHOLDER = "#resourceCode#"
        private const val RESOURCE_NAME_PLACEHOLDER = "#resourceName#"
    }
}
