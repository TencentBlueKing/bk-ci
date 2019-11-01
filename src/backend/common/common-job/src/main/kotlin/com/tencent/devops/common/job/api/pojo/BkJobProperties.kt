package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class BkJobProperties(
    val url: String? = null,
    val linkUrl: String? = null
)