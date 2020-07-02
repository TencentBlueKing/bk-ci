package com.tencent.devops.auth.utils

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

object ActionUtils {

    fun buildAction(resource: AuthResourceType, permission: AuthPermission) : String {
        return "${resource.value}_${permission.value}"
    }
}