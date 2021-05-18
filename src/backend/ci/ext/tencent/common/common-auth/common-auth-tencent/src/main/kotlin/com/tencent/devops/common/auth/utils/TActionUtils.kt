package com.tencent.devops.common.auth.utils

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

object TActionUtils {

    fun buildAction(authPermission: AuthPermission, authResourceType: AuthResourceType): String {
        return "${extResourceType(authResourceType)}_$authPermission"
    }

    fun extResourceType(authResourceType: AuthResourceType): String {
        val newResourceName = when(authResourceType) {
            AuthResourceType.QUALITY_GROUP -> ""
            AuthResourceType.QUALITY_RULE -> ""
            AuthResourceType.EXPERIENCE_TASK -> ""
            AuthResourceType.EXPERIENCE_GROUP -> ""
            else -> authResourceType.value
        }
        return newResourceName
    }
}
