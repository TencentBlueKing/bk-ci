package com.tencent.devops.turbo.pojo

data class TurboPlanStatModel(
    var sumEstimateTime: Double,
    var sumExecuteTime: Double,
    var turboRatio: String? = null
)
