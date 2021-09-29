package com.tencent.devops.turbo.vo

data class TurboPlanPageVO(
    val turboPlanList: List<TurboPlanStatRowVO>,
    val turboPlanCount: Long
)
