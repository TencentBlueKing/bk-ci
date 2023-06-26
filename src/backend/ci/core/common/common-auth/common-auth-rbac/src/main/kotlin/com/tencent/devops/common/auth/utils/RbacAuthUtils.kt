package com.tencent.devops.common.auth.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory

object RbacAuthUtils {
    private val logger = LoggerFactory.getLogger(RbacAuthUtils::class.java)
    private const val SYSTEM_PLACEHOLDER = "#system#"
    private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
    private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
    private const val RESOURCE_CODE_PLACEHOLDER = "#resourceCode#"
    private const val RESOURCE_NAME_PLACEHOLDER = "#resourceName#"

    fun buildAction(authPermission: AuthPermission, authResourceType: AuthResourceType): String {
        return "${extResourceType(authResourceType)}_${authPermission.value}"
    }

    fun getRelationResourceType(authPermission: AuthPermission, authResourceType: AuthResourceType): String {
        return if (authPermission == AuthPermission.CREATE) {
            AuthResourceType.PROJECT.value
        } else {
            authResourceType.value
        }
    }

    fun extResourceType(authResourceType: AuthResourceType): String {
        return when (authResourceType) {
            AuthResourceType.QUALITY_GROUP -> "quality_group"
            AuthResourceType.QUALITY_RULE -> "rule"
            AuthResourceType.EXPERIENCE_TASK -> "experience_task"
            AuthResourceType.EXPERIENCE_GROUP -> "experience_group"
            else -> authResourceType.value
        }
    }

    fun extResourceTypeCheck(authResourceType: String): Boolean {
        val extResourceTypeList = mutableListOf<String>()
        extResourceTypeList.add(AuthResourceType.QUALITY_GROUP.value)
        extResourceTypeList.add(AuthResourceType.QUALITY_RULE.value)
        extResourceTypeList.add(AuthResourceType.EXPERIENCE_TASK.value)
        extResourceTypeList.add(AuthResourceType.EXPERIENCE_GROUP.value)
        // 前端experience的枚举值为EXPERIENCE_TASK: experience, EXPERIENCE_GROUP: experienceGroup
        extResourceTypeList.add("experience")
        extResourceTypeList.add("experienceGroup")
        if (extResourceTypeList.contains(authResourceType)) {
            return true
        }
        return false
    }

    fun buildActionList(authPermissions: Set<AuthPermission>, authResourceType: AuthResourceType): List<String> {
        val actions = mutableListOf<String>()
        authPermissions.forEach {
            actions.add(buildAction(it, authResourceType))
        }
        return actions
    }

    fun getAuthPermissionByAction(action: String): AuthPermission {
        val permissionStr = action.substringAfterLast("_")
        return AuthPermission.get(permissionStr)
    }

    fun getResourceTypeByStr(resourceTypeStr: String): AuthResourceType {
        return when (resourceTypeStr) {
            "quality_group" -> AuthResourceType.QUALITY_GROUP
            "rule" -> AuthResourceType.QUALITY_RULE
            "experience_task" -> AuthResourceType.EXPERIENCE_TASK
            "experience_group" -> AuthResourceType.EXPERIENCE_GROUP
            else -> AuthResourceType.get(resourceTypeStr)
        }
    }

    fun buildResultMap(
        instancesMap: Map<AuthPermission, List<String>>
    ): Map<AuthPermission, List<Long>> {
        if (instancesMap.isEmpty())
            return emptyMap()
        val resultMap = mutableMapOf<AuthPermission, List<Long>>()
        instancesMap.forEach { (key, value) ->
            val instanceLongIds = mutableListOf<Long>()
            value.forEach {
                instanceLongIds.add(HashUtil.decodeIdToLong(it))
            }
            resultMap[key] = instanceLongIds
        }
        return resultMap
    }

    fun addSuffixIfNeed(resourceName: String, suffix: Int): String {
        return if (suffix == 0) resourceName else "$resourceName($suffix)"
    }

    /**
     * 构建分级管理员或分级管理员用户组授权范围
     */
    @Suppress("LongParameterList")
    fun buildAuthorizationScopes(
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
        logger.info("$projectCode authorization scopes after replace $replaceAuthorizationScopesStr ")
        return JsonUtil.to(replaceAuthorizationScopesStr, object : TypeReference<List<AuthorizationScopes>>() {})
    }
}
