package com.tencent.devops.common.job.api.pojo

import org.springframework.stereotype.Component

@Component
data class Step(
    val stepId: Int,
    val stepExt: StepExt
)