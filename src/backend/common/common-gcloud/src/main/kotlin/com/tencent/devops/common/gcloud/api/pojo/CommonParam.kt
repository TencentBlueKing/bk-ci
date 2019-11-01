package com.tencent.devops.common.gcloud.api.pojo

import org.springframework.stereotype.Component

@Component
data class CommonParam(
    val gameId: String,
    val accessId: String,
    val accessKey: String
)