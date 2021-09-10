package com.tencent.devops.turbo.pojo

import org.springframework.data.mongodb.core.mapping.Field
import java.time.LocalDate

data class TurboDaySummaryOverviewModel(
    @Field("project_id")
    val projectId: String?,

    @Field("estimate_time")
    val estimateTime: Double?,

    @Field("execute_time")
    val executeTime: Double?,

    @Field("execute_count")
    val executeCount: Int?,

    @Field("summary_day")
    val summaryDay: LocalDate?,

    @Field("plan_num")
    val instanceNum: Int?
)
