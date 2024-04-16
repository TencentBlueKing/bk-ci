package com.tencent.devops.common.auth.rbac.utils

import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory

object RbacAuthUtils {
    private val logger = LoggerFactory.getLogger(RbacAuthUtils::class.java)

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

    fun getAdditionalAction(): Map<String, List<Action>> {
        return mapOf(
            AuthResourceType.PROJECT.value to listOf(
                "project_visit", "quality_group_create", "env_node_create",
                "experience_task_create", "experience_group_create"
            ),
            AuthResourceType.QUALITY_RULE.value to listOf("rule_list"),
            AuthResourceType.QUALITY_GROUP_NEW.value to listOf("quality_group_list"),
            AuthResourceType.ENVIRONMENT_ENV_NODE.value to listOf("env_node_list"),
            AuthResourceType.EXPERIENCE_TASK_NEW.value to listOf("experience_task_list", "experience_task_view"),
            AuthResourceType.EXPERIENCE_GROUP_NEW.value to listOf("experience_group_list", "experience_group_view")
        ).mapValues { (_, actions) -> actions.map { Action(it) } }
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
}
