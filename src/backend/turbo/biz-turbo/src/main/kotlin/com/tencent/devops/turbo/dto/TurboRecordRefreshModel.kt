package com.tencent.devops.turbo.dto

import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDateTime

data class TurboRecordRefreshModel(
    @Field("created_date")
    val createdDate: LocalDateTime,
    @Field("estimate_time")
    val estimateTimeSecond: Long
)
