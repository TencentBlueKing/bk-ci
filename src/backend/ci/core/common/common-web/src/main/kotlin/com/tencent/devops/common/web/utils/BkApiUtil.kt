package com.tencent.devops.common.web.utils

import com.tencent.devops.common.web.constant.BkApiHandleType

object BkApiUtil {

    fun getApiAccessLimitProjectKey(): String {
        return "${BkApiHandleType.PROJECT_API_ACCESS_LIMIT}:projects"
    }
}
