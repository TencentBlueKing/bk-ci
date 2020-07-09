package com.tencent.devops.common.auth.utlis

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

object ActionUtils {

    fun buildAction(authResourceType: AuthResourceType, permission: AuthPermission): String {
        return "${authResourceType.value}_${permission.value}"
    }
}