package com.tencent.devops.common.gcloud.api.pojo

import org.springframework.stereotype.Component

@Component
data class BkGcloudProperties(
    val gcloudUrl: String? = null
)