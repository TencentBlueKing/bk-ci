package com.tencent.devops.turbo.dto

data class TurboRecordCreateDto(
    val engineCode: String,
    val dataMap: Map<String, Any?>
)
