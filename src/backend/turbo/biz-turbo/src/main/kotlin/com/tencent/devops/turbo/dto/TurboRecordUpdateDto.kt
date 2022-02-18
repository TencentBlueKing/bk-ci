package com.tencent.devops.turbo.dto

data class TurboRecordUpdateDto(
    val engineCode: String,
    val tbsTurboRecordId: String?,
    val buildId: String?,
    val turboPlanId: String?
)
