package com.tencent.devops.quality.api.v2.pojo

import io.swagger.annotations.ApiModel

@ApiModel("模板")
data class RuleTemplate(
    val hashId: String,
    val name: String,
    val desc: String,
    val indicators: List<QualityIndicator>,
    val stage: String,
    val controlPoint: String, // 控制点原子类型
    val controlPointName: String, // 控制点名称
    val controlPointPosition: ControlPointPosition, // 控制点红线位置
    val availablePosition: List<ControlPointPosition> // 控制点红线位置
)