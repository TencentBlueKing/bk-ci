package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class GlobalVar(
    val id: Int,
    val value: String
)