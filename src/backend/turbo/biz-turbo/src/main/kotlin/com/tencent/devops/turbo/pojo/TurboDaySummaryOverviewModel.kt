package com.tencent.devops.turbo.pojo

import com.tencent.devops.turbo.model.pojo.EngineSceneEntity
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
    val instanceNum: Int?,

    /**
     * 按加速场景的加速次数
     */
    @Field("engine_scene_list")
    val engineSceneList: List<EngineSceneEntity>

)
