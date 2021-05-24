package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

object TActionUtils {

    fun buildAction(authPermission: AuthPermission, authResourceType: AuthResourceType): String {
        return "${extResourceType(authResourceType)}_${authPermission.value}"
    }

    fun extResourceType(authResourceType: AuthResourceType): String {
        val newResourceName = when(authResourceType) {
            AuthResourceType.QUALITY_GROUP -> "quality_group"
            AuthResourceType.QUALITY_RULE -> "rule"
            AuthResourceType.EXPERIENCE_TASK -> "experience_task"
            AuthResourceType.EXPERIENCE_GROUP -> "experience_group"
            else -> authResourceType.value
        }
        return newResourceName
    }

    fun buildActionList(authPermissions : Set<AuthPermission>, authResourceType: AuthResourceType) : List<String> {
        val actions = mutableListOf<String>()
        authPermissions.forEach {
            actions.add(buildAction(it, authResourceType))
        }
        return actions
    }
}
