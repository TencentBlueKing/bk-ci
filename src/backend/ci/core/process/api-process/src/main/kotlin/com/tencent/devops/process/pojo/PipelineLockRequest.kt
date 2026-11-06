package com.tencent.devops.process.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线启用/禁用请求")
data class PipelineLockRequest(
    @get:Schema(title = "禁用原因，最多120个字符", required = false)
    val lockReason: String? = null
) {
    companion object {
        const val LOCK_REASON_MAX_LENGTH = 120
    }
}
