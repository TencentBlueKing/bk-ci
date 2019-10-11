package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import io.swagger.annotations.ApiModel

@ApiModel("指标分组响应")
data class IndicatorStageGroup(
    val hashId: String,
    val stage: String,
    val controlPoints: List<IndicatorControlPointGroup>
) {
    data class IndicatorControlPointGroup(
        val hashId: String,
        val controlPoint: String,
        val controlPointName: String,
        val details: List<IndicatorDetailGroup>
    )
    data class IndicatorDetailGroup(
        val hashId: String,
        val detail: String,
        val desc: String,
        val items: List<QualityIndicator>
    )
}
