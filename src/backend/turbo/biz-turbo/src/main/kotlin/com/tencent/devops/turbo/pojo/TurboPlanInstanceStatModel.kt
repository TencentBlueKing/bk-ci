package com.tencent.devops.turbo.pojo

data class TurboPlanInstanceStatModel(
    val averageEstimateTime: Long,
    val averageExecuteTime: Long,
    var averageExecuteTimeValue: String? = null,
    var turboRatio: String? = null
)
