package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.QualityControlPoint
import io.swagger.annotations.ApiModel

@ApiModel("控制点的分组响应")
data class ControlPointStageGroup(
    val stage: String,
    val controlPoints: List<QualityControlPoint>
)
