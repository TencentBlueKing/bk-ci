package com.tencent.devops.openapi.utils

import org.slf4j.LoggerFactory
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object ApigwParamUtil {
    fun standardSize(pageSize: Int?): Int? {
        val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        if (!request.getHeader(temporaryKey).isNullOrBlank()) {
            logger.warn("TEMPORARY-PAGE-SIZE-NOT-LIMIT, pageSize:$pageSize, path: ${request.requestURI}")
            return pageSize
        }

        if (pageSize != null && pageSize > 100) {
            logger.warn("page size is exceeded , pageSize:$pageSize, path: ${request.requestURI}")
            return 100
        }
        return pageSize
    }

    private val logger = LoggerFactory.getLogger(ApigwParamUtil::class.java)
    private const val temporaryKey = "X-DEVOPS-TEMPORARY-PAGE-SIZE-NOT-LIMIT"
}
