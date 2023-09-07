package com.tencent.devops.common.web.utils

import com.tencent.devops.common.web.constant.BkApiHandleType

object BkApiUtil {

    private val apiPermissionThreadLocal = ThreadLocal<Boolean>()

    fun getApiAccessLimitProjectKey(): String {
        return "${BkApiHandleType.PROJECT_API_ACCESS_LIMIT}:projects"
    }

    fun setPermissionFlag(permissionFlag: Boolean) {
        apiPermissionThreadLocal.set(permissionFlag)
    }

    fun getPermissionFlag(): Boolean? {
        return apiPermissionThreadLocal.get()
    }
}
