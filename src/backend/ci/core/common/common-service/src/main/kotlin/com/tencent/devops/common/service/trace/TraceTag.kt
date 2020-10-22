package com.tencent.devops.common.service.trace

import com.tencent.devops.common.api.util.UUIDUtil

object TraceTag {
    const val BIZID = "bizId"
    const val TRACE_HEADER_DEVOPS_BIZID = "X-DEVOPS-TRACE-BIZ-ID"
    const val TRACE_HEADER_DEVOPS_TRACE = "X-DEVOPS-TRACE-ID"
    const val BIZIDTAG = "biz"

    fun buildBiz(): String {
        return "$BIZIDTAG-${UUIDUtil.generate()}"
    }
}