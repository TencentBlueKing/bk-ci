package com.tencent.bk.codecc.task.vo

data class GrayTaskStatVO(
    val totalSerious: Int?,
    val totalNormal: Int?,
    val totalPrompt: Int?,
    val elapsedTime: Long?,
    val currStep: Int?,
    val flag: Int?
)
