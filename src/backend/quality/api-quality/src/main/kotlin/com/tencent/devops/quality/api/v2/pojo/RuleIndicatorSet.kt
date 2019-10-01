package com.tencent.devops.quality.api.v2.pojo

import io.swagger.annotations.ApiModel

@ApiModel("指标集")
data class RuleIndicatorSet(
    val hashId: String,
    val name: String,
    val desc: String,
    val indicators: List<QualityIndicator>
)