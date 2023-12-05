package com.tencent.devops.openapi.utils

import com.tencent.devops.common.api.exception.ParamBlankException

object ApigwParamUtil {
    fun checkPageSize(pageSize: Int?) {
        if (pageSize != null && pageSize > 100) {
            throw ParamBlankException("Page Size cannot exceed 100")
        }
    }
}
