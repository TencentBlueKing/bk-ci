package com.tencent.devops.openapi.utils

import org.slf4j.LoggerFactory

object ApigwParamUtil {
    fun standardSize(pageSize: Int?): Int? {
        if (pageSize != null && pageSize > 100) {
            logger.warn("page size is exceeded , pageSize:$pageSize")
            return 100
        }
        return pageSize
    }

    private val logger = LoggerFactory.getLogger(ApigwParamUtil::class.java)
}
