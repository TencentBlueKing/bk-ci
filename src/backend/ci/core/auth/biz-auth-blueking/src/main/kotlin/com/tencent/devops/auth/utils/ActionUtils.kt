package com.tencent.devops.auth.utils

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

object ActionUtils {

    fun buildAction(resource: AuthResourceType, permission: AuthPermission) : String {
        return "${resource.value}_${permission.value}"
    }

    fun actionType(action: String) : String {
        return if(action.contains("_")) {
            action.substringBefore("_")
        } else {
            action
        }
    }
}